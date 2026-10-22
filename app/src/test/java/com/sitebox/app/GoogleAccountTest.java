package com.sitebox.app;

import android.accounts.Account;
import android.app.Activity;
import android.content.Intent;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.concurrent.ThreadPoolExecutor;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** Exercises the real SDK result handler; fixtures do not claim a live Google login. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class GoogleAccountTest {
    static final String EMAIL="reader@example.com";
    static final String SCOPE="https://www.googleapis.com/auth/drive.appdata";
    Activity activity;
    GoogleDriveBackup drive;
    String identity="", status="";
    int identities;
    @Before public void setup() throws Exception {
        activity=Robolectric.buildActivity(Activity.class).setup().get();
        drive=new GoogleDriveBackup(activity,new GoogleDriveBackup.Listener(){
            public void onChanged(String s,String account,boolean busy){status=s;}
            public void onRestore(String json,String label){}
            public void onIdentity(String email,String label){identity=email;identities++;}
        });
        // The deliberately unavailable worker models Drive failing after Google authorization.
        ((ThreadPoolExecutor)get(drive,"worker")).shutdownNow();
        call(drive,"begin",new Class<?>[]{int.class,String.class},2,null);
    }
    @After public void close(){drive.close();activity.finish();}
    static Object get(Object target,String name)throws Exception{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}
    static void set(Object target,String name,Object value)throws Exception{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);f.set(target,value);}
    static Object call(Object target,String name,Class<?>[] types,Object... args)throws Exception{Method m=target.getClass().getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(target,args);}
    void accept(String email) throws Exception {
        GoogleSignInAccount account=GoogleSignInAccount.fromAccount(new Account(email,"com.google"));
        AuthorizationResult result=new AuthorizationResult(null,"fixture-token",null,Collections.singletonList(SCOPE),account,null);
        call(drive,"accept",new Class<?>[]{AuthorizationResult.class,int.class},result,(Integer)get(drive,"generation"));
    }
    @Test public void authorizedIdentityIsReportedEvenWhenDriveCannotStart()throws Exception{
        accept(EMAIL);
        assertEquals("Google identity must be known before Drive access",EMAIL,identity);
        assertEquals(EMAIL,drive.email());
        assertTrue(get(drive,"account").toString().contains(EMAIL));
        assertEquals(1,identities);
    }
    @Test public void exhaustedTokenRefreshRetainsConnectedAccount()throws Exception{
        set(drive,"email",EMAIL);set(drive,"account","Reader ("+EMAIL+")");set(drive,"retried",true);
        call(drive,"expired",new Class<?>[]{String.class},"fixture-expired-token");
        assertEquals("Token expiry is not sign-out",EMAIL,drive.email());
        assertTrue(get(drive,"account").toString().contains(EMAIL));
    }
    @Test public void wrongAccountResultCannotReplaceSavedAccount()throws Exception{
        set(drive,"email",EMAIL);set(drive,"account",EMAIL);
        accept("other@example.com");
        assertEquals(EMAIL,drive.email());
        assertEquals(0,identities);
        assertTrue("A mismatched Google result must fail closed",status.contains("계정"));
        assertFalse((Boolean)get(drive,"busy"));
    }
    @Test public void deniedDrivePermissionRetainsGoogleLogin()throws Exception{
        GoogleSignInAccount account=GoogleSignInAccount.fromAccount(new Account(EMAIL,"com.google"));
        AuthorizationResult result=new AuthorizationResult(null,"fixture-token",null,Collections.emptyList(),account,null);
        call(drive,"accept",new Class<?>[]{AuthorizationResult.class,int.class},result,(Integer)get(drive,"generation"));
        assertEquals(EMAIL,identity);assertEquals(EMAIL,drive.email());
        assertTrue(status.contains("권한"));assertFalse((Boolean)get(drive,"busy"));
        assertNull(get(drive,"token"));
    }
    Intent errorResult(int code){
        Intent data=new Intent();
        SafeParcelableSerializer.serializeToIntentExtra(new Status(code),data,"status");
        return data;
    }
    @Test public void cancelledActivityMustNotHideDeveloperError()throws Exception{
        set(drive,"awaiting",true);
        drive.onActivityResult(9130,Activity.RESULT_CANCELED,errorResult(10));
        assertTrue("OAuth configuration failure must remain distinguishable",status.contains("OAuth"));
        assertTrue(status.contains("10"));assertFalse(status.contains("취소했어요"));
        assertEquals("",identity);assertFalse((Boolean)get(drive,"busy"));
    }
    @Test public void cancelledActivityMustNotHideNetworkError()throws Exception{
        set(drive,"awaiting",true);
        drive.onActivityResult(9130,Activity.RESULT_CANCELED,errorResult(7));
        assertTrue(status.contains("7"));assertFalse(status.contains("취소했어요"));
        assertEquals("",identity);
    }
    @Test public void absentActivityDataDoesNotClaimUserCancellation()throws Exception{
        set(drive,"awaiting",true);
        drive.onActivityResult(9130,Activity.RESULT_CANCELED,null);
        assertFalse("Missing result data does not establish user cancellation",status.contains("취소했어요"));
        assertTrue(status.contains("화면 반환 코드 0"));
        assertEquals("",identity);
    }
    @Test public void successActivityWithoutGoogleDataIsNotSuccessfulLogin()throws Exception{
        set(drive,"awaiting",true);
        drive.onActivityResult(9130,Activity.RESULT_OK,null);
        assertFalse(status.contains("취소했어요"));assertTrue(status.contains("화면 반환 코드 -1"));
        assertEquals("",identity);assertNull(get(drive,"token"));
    }
    Intent successfulGoogleResult(){
        Intent data=errorResult(0);
        GoogleSignInAccount account=GoogleSignInAccount.fromAccount(new Account(EMAIL,"com.google"));
        AuthorizationResult result=new AuthorizationResult(null,"fixture-token",null,Collections.singletonList(SCOPE),account,null);
        SafeParcelableSerializer.serializeToIntentExtra(result,data,"authorization_result");
        return data;
    }
    @Test public void completedGoogleActivityUsesSdkResult()throws Exception{
        set(drive,"awaiting",true);
        assertTrue(drive.onActivityResult(9130,Activity.RESULT_OK,successfulGoogleResult()));
        assertEquals(EMAIL,identity);assertEquals(EMAIL,drive.email());
    }
    @Test public void cancelledActivityCannotAcceptEvenSuccessfulPayload()throws Exception{
        set(drive,"awaiting",true);
        drive.onActivityResult(9130,Activity.RESULT_CANCELED,successfulGoogleResult());
        assertEquals("",identity);assertNull(get(drive,"token"));
        assertTrue(status.contains("화면 반환 코드 0"));
    }
    @Test public void googleCancellationStatusRemainsDistinguishable()throws Exception{
        set(drive,"awaiting",true);
        drive.onActivityResult(9130,Activity.RESULT_CANCELED,errorResult(16));
        assertTrue(status.contains("인증 코드 16"));
        assertEquals("",identity);assertFalse((Boolean)get(drive,"busy"));
    }
    @Test public void cancelledGoogleConsentNeverInventsAccount()throws Exception{
        set(drive,"awaiting",true);
        assertTrue(drive.onActivityResult(9130,Activity.RESULT_CANCELED,null));
        assertEquals("",identity);assertEquals("",drive.email());
        assertFalse((Boolean)get(drive,"busy"));
    }
}
