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
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
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

/** Drive app-data transport. Access tokens stay in memory; silent resume never opens consent UI. */
final class GoogleDriveBackup {
 interface Listener {void onChanged(String status,String account,boolean busy);void onRestore(String json,String label);default void onIdentity(String email,String label){}default void onSaved(String json){}default void onFailure(){}default void onDisconnected(){}}
 private static final int REQUEST=9130,META_LIMIT=32*1024;
 private static final String SCOPE="https://www.googleapis.com/auth/drive.appdata",STATE="sitebox.drive.awaiting";
 private final Activity activity;private final Listener listener;private final AuthorizationClient auth;
 private final Handler main=new Handler(Looper.getMainLooper());
 private final ThreadPoolExecutor worker=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(1));
 private volatile boolean closed;private volatile HttpsURLConnection active;
 private boolean busy,awaiting,retried,interactive,tokenIdentity;private volatile int generation;private int operation;private String token,account="",email="",payload;
 private String cachedAccount="",cachedId="",cachedJson;
 private String status="Google 계정을 연결해 주세요.";private Runnable timeout;
 GoogleDriveBackup(Activity activity,Listener listener){this.activity=activity;this.listener=listener;auth=Identity.getAuthorizationClient(activity);}
 private void changed(String text){if(closed)return;status=text;listener.onChanged(status,account,busy);}
 private boolean begin(int op,String data){if(closed||busy)return false;busy=true;operation=op;payload=data;retried=false;generation++;changed("Google Drive 작업 중…");return true;}
 void connect(){if(begin(2,null)){interactive=true;email="";token=null;cachedAccount="";cachedId="";cachedJson=null;authorize(true);}}
 void resume(String savedEmail,boolean explicit){if(savedEmail==null||savedEmail.isEmpty())return;if(begin(2,null)){interactive=explicit;email=savedEmail;if(token!=null)runIo(generation);else authorize(false);}}
 String email(){return email;}
 void backup(String json){if(!begin(1,json))return;try{BackupCodec.decode(json);}catch(Exception e){fail("백업 데이터를 확인해 주세요.");return;}if(token==null)authorize(false);else runIo(generation);}
 void restoreLatest(){if(!begin(2,null))return;if(token==null)authorize(false);else runIo(generation);}
 private AuthorizationRequest request(boolean choose){AuthorizationRequest.Builder b=AuthorizationRequest.builder().setRequestedScopes(Collections.singletonList(new Scope(SCOPE))).setOptOutIncludingGrantedScopes(true);if(choose)b.setPrompt(AuthorizationRequest.Prompt.SELECT_ACCOUNT);else if(!email.isEmpty())b.setAccount(new Account(email,"com.google"));return b.build();}
 private void authorize(boolean choose){final int gen=generation;armTimeout(gen);auth.authorize(request(choose)).addOnSuccessListener(result->{if(valid(gen))accept(result,gen);}).addOnFailureListener(error->{if(valid(gen))authError(error);});}
 private void accept(AuthorizationResult result,int gen){
  if(result.hasResolution()){
   if(!interactive){fail("Google 계정을 다시 연결하면 자동 동기화가 이어집니다.");return;}
   awaiting=true;cancelTimeout();try{activity.startIntentSenderForResult(result.getPendingIntent().getIntentSender(),REQUEST,null,0,0,0);}catch(Exception e){awaiting=false;fail("Google 연결 화면을 열지 못했어요.");}return;
  }
  awaiting=false;
  // Google authorization and Drive availability are separate states. Persist the
  // SDK-verified identity before any Drive request (which may fail with 403/offline).
  GoogleSignInAccount signedIn=result.toGoogleSignInAccount();tokenIdentity=false;
  if(signedIn!=null&&signedIn.getEmail()!=null&&!signedIn.getEmail().trim().isEmpty()){
   String verifiedEmail=signedIn.getEmail();
   if(!email.isEmpty()&&!email.equalsIgnoreCase(verifiedEmail)){fail("선택한 Google 계정이 연결된 계정과 달라요. 연결 해제 후 다른 계정으로 로그인해 주세요.");return;}
   rememberIdentity(verifiedEmail,signedIn.getDisplayName());tokenIdentity=true;
  }
  if(!result.getGrantedScopes().contains(SCOPE)||result.getAccessToken()==null||result.getAccessToken().isEmpty()){fail("Google Drive 자동 동기화 권한이 필요해요. Google 계정을 다시 연결해 주세요.");return;}
  token=result.getAccessToken();if(account.isEmpty())account="Google 인증 완료 · 계정 정보 확인 중";changed("Google 계정을 확인하고 있어요.");runIo(gen);
 }
 private void rememberIdentity(String verifiedEmail,String name){email=verifiedEmail;account=(name==null||name.trim().isEmpty()||name.equals(verifiedEmail))?verifiedEmail:name+" ("+verifiedEmail+")";listener.onIdentity(email,account);}
 boolean onActivityResult(int req,int result,Intent data){
  if(req!=REQUEST)return false;if(closed||!awaiting)return true;awaiting=false;
  if(data==null){incompleteResult(result);return true;}
  // A canceled Activity can carry the real Google failure (e.g. OAuth code 10).
  // Let Google's SDK decode it before interpreting the Activity return code.
  try{
   AuthorizationResult authorization=auth.getAuthorizationResultFromIntent(data);
   if(result!=Activity.RESULT_OK)incompleteResult(result);else accept(authorization,generation);
  }catch(ApiException e){authError(e);}return true;
 }
 private void incompleteResult(int result){fail("Google 로그인이 완료되지 않았어요. 계정 선택 화면이 닫혔거나 인증 결과를 받지 못했습니다. 다시 시도해 주세요. (화면 반환 코드 "+result+")");}
 private void authError(Exception e){
  if(!(e instanceof ApiException)){fail("Google 계정 연결에 실패했어요. Google Play 서비스와 네트워크를 확인한 뒤 다시 시도해 주세요.");return;}
  int code=((ApiException)e).getStatusCode();String detail=" (인증 코드 "+code+")";
  if(code==10)fail("Google OAuth 설정이 필요해요. 개발자가 이 APK의 패키지 이름과 서명 SHA-1을 같은 Google Cloud 프로젝트에 등록해야 합니다."+detail);
  else if(code==16||code==12501)fail("Google 로그인이 완료되지 않았어요. 계정 선택·권한 동의 화면이 닫혔거나 Google에서 연결을 중단했습니다. 다시 시도해 주세요."+detail);
  else if(code==7)fail("Google 로그인 중 네트워크 오류가 발생했어요. 인터넷 연결을 확인한 뒤 다시 시도해 주세요."+detail);
  else fail("Google 계정 연결에 실패했어요. Google Play 서비스와 개발자 OAuth 설정을 확인해 주세요."+detail);
 }
 private boolean valid(int gen){return !closed&&busy&&gen==generation;}
 private void armTimeout(int gen){cancelTimeout();timeout=()->{if(valid(gen)){generation++;HttpsURLConnection c=active;if(c!=null)c.disconnect();fail("연결 시간이 초과됐어요. 다시 시도해 주세요.");}};main.postDelayed(timeout,45000);}
 private void cancelTimeout(){if(timeout!=null){main.removeCallbacks(timeout);timeout=null;}}
 private void fail(String text){finish(text);if(!closed)listener.onFailure();}
 private void finish(String text){cancelTimeout();busy=false;awaiting=false;payload=null;changed(text);}
 private void runIo(int gen){
  armTimeout(gen);final String bearer=token,data=payload,expectedEmail=email,knownAccount=account;final boolean identityKnown=tokenIdentity;final int op=operation;
  try{worker.execute(()->{
   try{
    IoContext io=new IoContext(gen);
    final String verifiedEmail,name;
    if(identityKnown){verifiedEmail=expectedEmail;name=knownAccount;}
    else{
     // Compatibility fallback for older Play services without an account in the result.
     JSONObject user=new JSONObject(http("GET","https://www.googleapis.com/drive/v3/about?fields=user(displayName,emailAddress)",null,null,bearer,META_LIMIT,io)).getJSONObject("user");
     verifiedEmail=user.getString("emailAddress");name=user.optString("displayName",verifiedEmail);
     if(verifiedEmail.trim().isEmpty()||(!expectedEmail.isEmpty()&&!expectedEmail.equalsIgnoreCase(verifiedEmail)))throw new IOException();
     main.post(()->{if(valid(gen)){rememberIdentity(verifiedEmail,name);tokenIdentity=true;}});
    }
    main.post(()->{if(valid(gen))changed("Google 계정 연결 완료 · 서재를 동기화하고 있어요.");});
    String restored=null,label=null;
    if(op==1){
     BackupCodec.decode(data);
     SimpleDateFormat stamp=new SimpleDateFormat("yyyyMMdd'T'HHmmssSSS'Z'",Locale.US);stamp.setTimeZone(TimeZone.getTimeZone("UTC"));
     String filename="sitebox-"+stamp.format(new Date())+"-"+UUID.randomUUID()+".json";
     JSONObject metadata=new JSONObject().put("name",filename).put("mimeType","application/json").put("parents",new JSONArray().put("appDataFolder")).put("appProperties",new JSONObject().put("kind","siteboxBackup"));
     String boundary="sitebox_"+UUID.randomUUID().toString().replace("-","");
     String body="--"+boundary+"\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n"+metadata+"\r\n--"+boundary+"\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n"+data+"\r\n--"+boundary+"--\r\n";
     JSONObject uploaded=new JSONObject(http("POST","https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id",body.getBytes(StandardCharsets.UTF_8),"multipart/related; boundary="+boundary,bearer,META_LIMIT,io));
     if(uploaded.getString("id").isEmpty())throw new IOException();cachedAccount=verifiedEmail;cachedId=uploaded.getString("id");cachedJson=data;
    }else if(op==2){
     String query="trashed = false and appProperties has { key='kind' and value='siteboxBackup' }";
     JSONObject listing=new JSONObject(http("GET","https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&orderBy=createdTime%20desc&pageSize=1&fields=files(id,name,createdTime)&q="+URLEncoder.encode(query,"UTF-8"),null,null,bearer,META_LIMIT,io));
     JSONArray files=listing.getJSONArray("files");if(files.length()==0){main.post(()->{if(valid(gen)){finish("저장된 서재가 없어 기기 내용을 자동 저장합니다.");listener.onRestore(null,verifiedEmail);}});return;}
     JSONObject file=files.getJSONObject(0);String id=file.getString("id");if(!id.matches("[A-Za-z0-9_-]+"))throw new IOException();
     if(id.equals(cachedId)&&verifiedEmail.equals(cachedAccount)&&cachedJson!=null)restored=cachedJson;else{restored=http("GET","https://www.googleapis.com/drive/v3/files/"+id+"?alt=media",null,null,bearer,BackupCodec.MAX_BYTES,io);BackupCodec.decode(restored);cachedAccount=verifiedEmail;cachedId=id;cachedJson=restored;}label=file.getString("name");
    }
    final String json=restored,restoreLabel=label;
    main.post(()->{if(valid(gen)){finish(op==1?"Google Drive에 서재를 자동 저장했어요.":op==2?"저장된 서재를 확인했어요.":"Google 계정이 연결됐어요.");if(!closed){if(op==2)listener.onRestore(json,verifiedEmail);else if(op==1)listener.onSaved(data);}}});
   }catch(Unauthorized e){main.post(()->{if(valid(gen))expired(bearer);});}
   catch(Exception e){final String reason=e instanceof DriveError?((DriveError)e).userMessage():"Google Drive 연결에 실패했어요. 기기 데이터는 유지되며 자동으로 재시도합니다.";main.post(()->{if(valid(gen))fail(reason);});}
  });}catch(java.util.concurrent.RejectedExecutionException e){fail("이전 작업이 끝난 뒤 다시 시도해 주세요.");}
 }
 private void expired(String expired){
  token=null;tokenIdentity=false;if(retried){fail("Google 계정을 다시 연결해 주세요. 기기 데이터는 유지됩니다.");return;}retried=true;changed("Google 계정 인증을 갱신하고 있어요.");final int gen=generation;
  auth.clearToken(ClearTokenRequest.builder().setToken(expired).build()).addOnSuccessListener(unused->{if(valid(gen))authorize(false);}).addOnFailureListener(error->{if(valid(gen))fail("Google 계정을 다시 연결해 주세요.");});
 }
 void disconnect(){
  if(!begin(3,null))return;final int gen=generation;armTimeout(gen);
  RevokeAccessRequest.Builder b=RevokeAccessRequest.builder().setScopes(Collections.singletonList(new Scope(SCOPE)));if(!email.isEmpty())b.setAccount(new Account(email,"com.google"));
  auth.revokeAccess(b.build()).addOnSuccessListener(unused->{if(valid(gen)){token=null;account="";email="";finish("Google 계정 연결을 해제했어요.");listener.onDisconnected();}}).addOnFailureListener(error->{if(valid(gen))fail("연결 해제에 실패했어요. 다시 시도해 주세요.");});
 }
 void saveState(Bundle out){out.putBoolean(STATE,awaiting);out.putInt(STATE+".op",operation);}
 void restoreState(Bundle state){if(state!=null&&state.getBoolean(STATE,false)&&!closed){awaiting=true;busy=true;operation=state.getInt(STATE+".op",2);interactive=true;payload=null;generation++;changed("Google 연결 결과를 기다리고 있어요.");armTimeout(generation);}}
 void close(){closed=true;cachedJson=null;cachedAccount="";cachedId="";generation++;token=null;payload=null;account="";email="";cancelTimeout();main.removeCallbacksAndMessages(null);HttpsURLConnection c=active;if(c!=null)c.disconnect();worker.shutdownNow();}
 private static final class IoContext{final int generation;final long deadline;IoContext(int gen){generation=gen;deadline=SystemClock.elapsedRealtime()+45000;}}
 private void checkIo(IoContext io)throws IOException{if(closed||generation!=io.generation||Thread.currentThread().isInterrupted()||SystemClock.elapsedRealtime()>=io.deadline)throw new IOException();}
 private static final class Unauthorized extends IOException {}
 private static final class DriveError extends IOException {
  final int code;final String reason;DriveError(int code,String reason){this.code=code;this.reason=reason;}
  String userMessage(){if(code==403&&(reason.equals("accessNotConfigured")||reason.equals("serviceDisabled")))return "Google 인증은 완료됐지만 개발자가 Google Drive API를 사용 설정해야 합니다. (HTTP 403)";if(code==403)return "Google 계정의 Drive 접근이 거부됐어요. 권한과 OAuth 설정을 확인해 주세요. (HTTP 403)";if(code==429||code>=500)return "Google Drive가 일시적으로 응답하지 않아요. 잠시 뒤 자동 재시도합니다. (HTTP "+code+")";return "Drive 동기화 실패 (HTTP "+code+"). 기기 데이터는 유지됩니다.";}
 }
 private String http(String method,String address,byte[] body,String contentType,String bearer,int limit,IoContext io)throws Exception{
  checkIo(io);
  URL url=new URL(address);if(!"https".equals(url.getProtocol())||!"www.googleapis.com".equals(url.getHost())||url.getPort()!=-1||url.getUserInfo()!=null)throw new IOException();
  HttpsURLConnection c=(HttpsURLConnection)url.openConnection();active=c;
  try{
   checkIo(io);c.setInstanceFollowRedirects(false);c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setRequestMethod(method);c.setRequestProperty("Authorization","Bearer "+bearer);c.setRequestProperty("Accept","application/json");
   if(body!=null){c.setDoOutput(true);c.setFixedLengthStreamingMode(body.length);c.setRequestProperty("Content-Type",contentType);try(OutputStream out=c.getOutputStream()){checkIo(io);out.write(body);}}
   int code=c.getResponseCode();if(code==401)throw new Unauthorized();if(code<200||code>=300){String reason="";try(InputStream error=c.getErrorStream()){if(error!=null){ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[1024];int n;while((n=error.read(buffer))!=-1){checkIo(io);if(bytes.size()+n>META_LIMIT)break;bytes.write(buffer,0,n);}JSONObject details=new JSONObject(bytes.toString("UTF-8")).optJSONObject("error");if(details!=null){JSONArray errors=details.optJSONArray("errors");if(errors!=null&&errors.length()>0)reason=errors.getJSONObject(0).optString("reason");}}}catch(Exception ignored){}throw new DriveError(code,reason);}if(c.getContentLengthLong()>limit)throw new IOException();
   ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){checkIo(io);if(bytes.size()+n>limit)throw new IOException();bytes.write(buffer,0,n);}}
   return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes.toByteArray())).toString();
  }finally{c.disconnect();if(active==c)active=null;}
 }
}
