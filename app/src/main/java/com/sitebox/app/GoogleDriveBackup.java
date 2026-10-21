package com.sitebox.app;

import android.accounts.Account;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import com.google.android.gms.auth.api.identity.AuthorizationClient;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.ClearTokenRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.RevokeAccessRequest;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HttpsURLConnection;

/** Manual Drive app-data snapshots. Tokens and pending data live only in this instance. */
final class GoogleDriveBackup {
 interface Listener {void onChanged(String status,String account,boolean busy);void onRestore(String json,String label);}
 private static final int REQUEST=9130,META_LIMIT=32*1024;
 private static final String SCOPE="https://www.googleapis.com/auth/drive.appdata",STATE="sitebox.drive.awaiting";
 private final Activity activity;private final Listener listener;private final AuthorizationClient auth;
 private final Handler main=new Handler(Looper.getMainLooper());
 private final ThreadPoolExecutor worker=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(1));
 private volatile boolean closed;private volatile HttpsURLConnection active;
 private boolean busy,awaiting,retried;private volatile int generation;private int operation;private String token,account="",email="",payload;
 private String status="Google 계정을 연결해 주세요.";private Runnable timeout;
 GoogleDriveBackup(Activity activity,Listener listener){this.activity=activity;this.listener=listener;auth=Identity.getAuthorizationClient(activity);}
 private void changed(String text){if(closed)return;status=text;listener.onChanged(status,account,busy);}
 private boolean begin(int op,String data){if(closed||busy)return false;busy=true;operation=op;payload=data;retried=false;generation++;changed("Google Drive 작업 중…");return true;}
 void connect(){if(begin(0,null))authorize(true);}
 void backup(String json){if(!begin(1,json))return;try{BackupCodec.decode(json);}catch(Exception e){finish("백업 데이터를 확인해 주세요.");return;}if(token==null)authorize(false);else runIo(generation);}
 void restoreLatest(){if(!begin(2,null))return;if(token==null)authorize(false);else runIo(generation);}
 private AuthorizationRequest request(boolean choose){AuthorizationRequest.Builder b=AuthorizationRequest.builder().setRequestedScopes(Collections.singletonList(new Scope(SCOPE))).setOptOutIncludingGrantedScopes(true);if(choose)b.setPrompt(AuthorizationRequest.Prompt.SELECT_ACCOUNT);else if(!email.isEmpty())b.setAccount(new Account(email,"com.google"));return b.build();}
 private void authorize(boolean choose){final int gen=generation;armTimeout(gen);auth.authorize(request(choose)).addOnSuccessListener(result->{if(valid(gen))accept(result,gen);}).addOnFailureListener(error->{if(valid(gen))authError(error);});}
 private void accept(AuthorizationResult result,int gen){
  if(result.hasResolution()){
   awaiting=true;cancelTimeout();try{activity.startIntentSenderForResult(result.getPendingIntent().getIntentSender(),REQUEST,null,0,0,0);}catch(Exception e){awaiting=false;finish("Google 연결 화면을 열지 못했어요.");}return;
  }
  awaiting=false;if(!result.getGrantedScopes().contains(SCOPE)||result.getAccessToken()==null||result.getAccessToken().isEmpty()){finish("앱 데이터 백업 권한이 필요해요.");return;}
  token=result.getAccessToken();account="";email="";changed("Google 계정을 확인하고 있어요.");runIo(gen);
 }
 boolean onActivityResult(int req,int result,Intent data){
  if(req!=REQUEST)return false;if(closed||!awaiting)return true;awaiting=false;
  if(result!=Activity.RESULT_OK||data==null){finish("Google 연결을 취소했어요.");return true;}
  try{accept(auth.getAuthorizationResultFromIntent(data),generation);}catch(ApiException e){authError(e);}return true;
 }
 private void authError(Exception e){if(e instanceof ApiException&&((ApiException)e).getStatusCode()==10)finish("Google OAuth 설정이 필요해요. 패키지 이름과 서명 SHA-1을 확인해 주세요.");else if(e instanceof ApiException&&((ApiException)e).getStatusCode()==12501)finish("Google 연결을 취소했어요.");else finish("Google 계정 연결에 실패했어요. 다시 시도해 주세요.");}
 private boolean valid(int gen){return !closed&&busy&&gen==generation;}
 private void armTimeout(int gen){cancelTimeout();timeout=()->{if(valid(gen)){generation++;HttpsURLConnection c=active;if(c!=null)c.disconnect();finish("연결 시간이 초과됐어요. 다시 시도해 주세요.");}};main.postDelayed(timeout,45000);}
 private void cancelTimeout(){if(timeout!=null){main.removeCallbacks(timeout);timeout=null;}}
 private void finish(String text){cancelTimeout();busy=false;awaiting=false;payload=null;changed(text);}
 private void runIo(int gen){
  armTimeout(gen);final String bearer=token,data=payload;final int op=operation;
  try{worker.execute(()->{
   try{
    IoContext io=new IoContext(gen);
    JSONObject user=new JSONObject(http("GET","https://www.googleapis.com/drive/v3/about?fields=user(displayName,emailAddress)",null,null,bearer,META_LIMIT,io)).getJSONObject("user");
    String verifiedEmail=user.getString("emailAddress"),name=user.optString("displayName",verifiedEmail);if(verifiedEmail.trim().isEmpty())throw new IOException();
    String restored=null,label=null;
    if(op==1){
     BackupCodec.decode(data);
     SimpleDateFormat stamp=new SimpleDateFormat("yyyyMMdd'T'HHmmssSSS'Z'",Locale.US);stamp.setTimeZone(TimeZone.getTimeZone("UTC"));
     String filename="sitebox-"+stamp.format(new Date())+"-"+UUID.randomUUID()+".json";
     JSONObject metadata=new JSONObject().put("name",filename).put("mimeType","application/json").put("parents",new JSONArray().put("appDataFolder")).put("appProperties",new JSONObject().put("kind","siteboxBackup"));
     String boundary="sitebox_"+UUID.randomUUID().toString().replace("-","");
     String body="--"+boundary+"\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n"+metadata+"\r\n--"+boundary+"\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n"+data+"\r\n--"+boundary+"--\r\n";
     JSONObject uploaded=new JSONObject(http("POST","https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id",body.getBytes(StandardCharsets.UTF_8),"multipart/related; boundary="+boundary,bearer,META_LIMIT,io));
     if(uploaded.getString("id").isEmpty())throw new IOException();
    }else if(op==2){
     String query="trashed = false and appProperties has { key='kind' and value='siteboxBackup' }";
     JSONObject listing=new JSONObject(http("GET","https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&orderBy=createdTime%20desc&pageSize=1&fields=files(id,name,createdTime)&q="+URLEncoder.encode(query,"UTF-8"),null,null,bearer,META_LIMIT,io));
     JSONArray files=listing.getJSONArray("files");if(files.length()==0){main.post(()->{if(valid(gen)){email=verifiedEmail;account=name+" ("+email+")";finish("저장된 SiteBox 백업이 없어요.");}});return;}
     JSONObject file=files.getJSONObject(0);String id=file.getString("id");if(!id.matches("[A-Za-z0-9_-]+"))throw new IOException();
     restored=http("GET","https://www.googleapis.com/drive/v3/files/"+id+"?alt=media",null,null,bearer,BackupCodec.MAX_BYTES,io);BackupCodec.decode(restored);label=file.getString("name");
    }
    final String json=restored,restoreLabel=label;
    main.post(()->{if(valid(gen)){email=verifiedEmail;account=name+" ("+email+")";finish(op==1?"Google Drive에 새 백업을 저장했어요.":op==2?"백업을 확인했어요.":"Google 계정이 연결됐어요.");if(json!=null&&!closed)listener.onRestore(json,restoreLabel);}});
   }catch(Unauthorized e){main.post(()->{if(valid(gen))expired(bearer);});}
   catch(Exception e){main.post(()->{if(valid(gen))finish("Google Drive 작업에 실패했어요. 연결과 백업 파일을 확인해 주세요.");});}
  });}catch(java.util.concurrent.RejectedExecutionException e){finish("이전 작업이 끝난 뒤 다시 시도해 주세요.");}
 }
 private void expired(String expired){
  token=null;account="";if(retried){email="";finish("Google 계정을 다시 연결해 주세요.");return;}retried=true;changed("Google 계정 인증을 갱신하고 있어요.");final int gen=generation;
  auth.clearToken(ClearTokenRequest.builder().setToken(expired).build()).addOnSuccessListener(unused->{if(valid(gen))authorize(false);}).addOnFailureListener(error->{if(valid(gen))finish("Google 계정을 다시 연결해 주세요.");});
 }
 void disconnect(){
  if(!begin(3,null))return;final int gen=generation;armTimeout(gen);
  RevokeAccessRequest.Builder b=RevokeAccessRequest.builder().setScopes(Collections.singletonList(new Scope(SCOPE)));if(!email.isEmpty())b.setAccount(new Account(email,"com.google"));
  auth.revokeAccess(b.build()).addOnSuccessListener(unused->{if(valid(gen)){token=null;account="";email="";finish("Google 계정 연결을 해제했어요.");}}).addOnFailureListener(error->{if(valid(gen))finish("연결 해제에 실패했어요. 다시 시도해 주세요.");});
 }
 void saveState(Bundle out){out.putBoolean(STATE,awaiting);}
 void restoreState(Bundle state){if(state!=null&&state.getBoolean(STATE,false)&&!closed){awaiting=true;busy=true;operation=0;payload=null;generation++;changed("Google 연결 결과를 기다리고 있어요.");armTimeout(generation);}}
 void close(){closed=true;generation++;token=null;payload=null;account="";email="";cancelTimeout();main.removeCallbacksAndMessages(null);HttpsURLConnection c=active;if(c!=null)c.disconnect();worker.shutdownNow();}
 private static final class IoContext{final int generation;final long deadline;IoContext(int gen){generation=gen;deadline=SystemClock.elapsedRealtime()+45000;}}
 private void checkIo(IoContext io)throws IOException{if(closed||generation!=io.generation||Thread.currentThread().isInterrupted()||SystemClock.elapsedRealtime()>=io.deadline)throw new IOException();}
 private static final class Unauthorized extends IOException {}
 private String http(String method,String address,byte[] body,String contentType,String bearer,int limit,IoContext io)throws Exception{
  checkIo(io);
  URL url=new URL(address);if(!"https".equals(url.getProtocol())||!"www.googleapis.com".equals(url.getHost())||url.getPort()!=-1||url.getUserInfo()!=null)throw new IOException();
  HttpsURLConnection c=(HttpsURLConnection)url.openConnection();active=c;
  try{
   checkIo(io);c.setInstanceFollowRedirects(false);c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setRequestMethod(method);c.setRequestProperty("Authorization","Bearer "+bearer);c.setRequestProperty("Accept","application/json");
   if(body!=null){c.setDoOutput(true);c.setFixedLengthStreamingMode(body.length);c.setRequestProperty("Content-Type",contentType);try(OutputStream out=c.getOutputStream()){checkIo(io);out.write(body);}}
   int code=c.getResponseCode();if(code==401)throw new Unauthorized();if(code<200||code>=300)throw new IOException();if(c.getContentLengthLong()>limit)throw new IOException();
   ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){checkIo(io);if(bytes.size()+n>limit)throw new IOException();bytes.write(buffer,0,n);}}
   return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes.toByteArray())).toString();
  }finally{c.disconnect();if(active==c)active=null;}
 }
}
