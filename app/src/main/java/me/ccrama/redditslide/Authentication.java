package me.ccrama.redditslide;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.afollestad.materialdialogs.AlertDialogWrapper;

import net.dean.jraw.RedditClient;
import net.dean.jraw.http.LoggingMode;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.http.OkHttpAdapter;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.http.oauth.OAuthHelper;
import net.dean.jraw.models.LoggedInAccount;

import java.util.Calendar;
import java.util.HashSet;
import java.util.UUID;

import me.ccrama.redditslide.util.LogUtil;
import me.ccrama.redditslide.util.NetworkUtil;
import okhttp3.Protocol;

/**
 * Created by ccrama on 3/30/2015.
 */
public class Authentication {
    private static final String CLIENT_ID = "KI2Nl9A_ouG9Qw";
    private static final String REDIRECT_URL = "http://www.ccrama.me";
    public static boolean isLoggedIn;
    public static RedditClient reddit;
    public static LoggedInAccount me;
    public static boolean mod;
    public static String name;
    public static SharedPreferences authentication;
    public static String refresh;

    public boolean hasDone;
    public static boolean didOnline;
    private static OkHttpAdapter httpAdapter;

    public static void resetAdapter() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                if (httpAdapter != null && httpAdapter.getNativeClient() != null)
                    httpAdapter.getNativeClient().connectionPool().evictAll();
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public Authentication(Context context) {
        Reddit.setDefaultErrorHandler(context);

        if (NetworkUtil.isConnected(context)) {
            hasDone = true;
            httpAdapter = new OkHttpAdapter(Reddit.client, Protocol.SPDY_3);
            isLoggedIn = false;
            reddit = new RedditClient(
                    UserAgent.of("android:me.ccrama.RedditSlide:v" + BuildConfig.VERSION_NAME), httpAdapter);
            reddit.setRetryLimit(3);
            if (BuildConfig.DEBUG) reddit.setLoggingMode(LoggingMode.ALWAYS);
            didOnline = true;
            new VerifyCredentials(context).execute();
        } else {
            isLoggedIn = Reddit.appRestart.getBoolean("loggedin", false);
            name = Reddit.appRestart.getString("name", "");
            if ((name.isEmpty() || !isLoggedIn) && !authentication.getString("lasttoken", "")
                    .isEmpty()) {
                for (String s : Authentication.authentication.getStringSet("accounts",
                        new HashSet<String>())) {
                    if (s.contains(authentication.getString("lasttoken", ""))) {
                        name = (s.split(":")[0]);
                        break;
                    }
                }
                isLoggedIn = true;
            }
        }


    }


    public void updateToken(Context c) {
        if (reddit == null) {
            hasDone = true;
            isLoggedIn = false;
            reddit = new RedditClient(
                    UserAgent.of("android:me.ccrama.RedditSlide:v" + BuildConfig.VERSION_NAME));
            reddit.setLoggingMode(LoggingMode.ALWAYS);
            didOnline = true;

            new VerifyCredentials(c).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            new UpdateToken(c).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    public static boolean authedOnce;

    public class UpdateToken extends AsyncTask<Void, Void, Void> {

        Context context;

        public UpdateToken(Context c) {
            this.context = c;
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (authedOnce && NetworkUtil.isConnected(context)) {
                didOnline = true;
                if (name != null && !name.isEmpty()) {
                    Log.v(LogUtil.getTag(), "REAUTH");
                    if (isLoggedIn) {
                        try {

                            final Credentials credentials =
                                    Credentials.installedApp(CLIENT_ID, REDIRECT_URL);
                            Log.v(LogUtil.getTag(), "REAUTH LOGGED IN");

                            OAuthHelper oAuthHelper = reddit.getOAuthHelper();

                            oAuthHelper.setRefreshToken(refresh);
                            OAuthData finalData;
                            if (authentication.contains("backedCreds")
                                    && authentication.getLong("expires", 0) > Calendar.getInstance()
                                    .getTimeInMillis()) {
                                finalData = oAuthHelper.refreshToken(credentials,
                                        authentication.getString("backedCreds",
                                                "")); //does a request
                            } else {
                                finalData = oAuthHelper.refreshToken(credentials); //does a request
                                authentication.edit()
                                        .putLong("expires",
                                                Calendar.getInstance().getTimeInMillis() + 3000000)
                                        .apply();
                            }
                            authentication.edit()
                                    .putString("backedCreds", finalData.getDataNode().toString())
                                    .apply();
                            reddit.authenticate(finalData);
                            refresh = oAuthHelper.getRefreshToken();
                            refresh = reddit.getOAuthHelper().getRefreshToken();

                            if (reddit.isAuthenticated()) {
                                if (me == null) {
                                    me = reddit.me();
                                }
                                Reddit.over18 = me.isOver18();
                                Authentication.isLoggedIn = true;

                            }
                            Log.v(LogUtil.getTag(), "AUTHENTICATED");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    } else {
                        final Credentials fcreds =
                                Credentials.userlessApp(CLIENT_ID, UUID.randomUUID());
                        OAuthData authData;
                        LogUtil.v("Not logged in");
                        try {

                            authData = reddit.getOAuthHelper().easyAuth(fcreds);
                            authentication.edit()
                                    .putLong("expires",
                                            Calendar.getInstance().getTimeInMillis() + 3000000)
                                    .apply();
                            authentication.edit()
                                    .putString("backedCreds", authData.getDataNode().toString())
                                    .apply();
                            Authentication.name = "LOGGEDOUT";
                            mod = false;

                            reddit.authenticate(authData);
                            Log.v(LogUtil.getTag(), "REAUTH LOGGED IN");

                        } catch (Exception e) {
                            try {
                                ((Activity) context).runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {

                                            new AlertDialogWrapper.Builder(context).setTitle(
                                                    R.string.err_general)
                                                    .setMessage(R.string.err_no_connection)
                                                    .setPositiveButton(R.string.btn_yes,
                                                            new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(
                                                                        DialogInterface dialog,
                                                                        int which) {
                                                                    new UpdateToken(
                                                                            context).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                                                                }
                                                            })
                                                    .setNegativeButton(R.string.btn_no,
                                                            new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(
                                                                        DialogInterface dialog,
                                                                        int which) {
                                                                    Reddit.forceRestart(context);

                                                                }
                                                            })
                                                    .show();
                                        } catch (Exception ignored) {

                                        }
                                    }
                                });
                            } catch (Exception e2) {
                                Toast.makeText(context,
                                        "Reddit could not be reached. Try again soon",
                                        Toast.LENGTH_SHORT).show();
                            }

                            //TODO fail
                        }
                    }


                }

            }
            return null;

        }

    }


    public class VerifyCredentials extends AsyncTask<String, Void, Void> {
        Context mContext;

        public VerifyCredentials(Context context) {
            mContext = context;
        }


        @Override
        protected Void doInBackground(String... subs) {
            try {

                String token = authentication.getString("lasttoken", "");
                if (BuildConfig.DEBUG) LogUtil.v("TOKEN IS " + token);
                if (!token.isEmpty()) {

                    Credentials credentials = Credentials.installedApp(CLIENT_ID, REDIRECT_URL);
                    OAuthHelper oAuthHelper = reddit.getOAuthHelper();
                    oAuthHelper.setRefreshToken(token);

                    try {
                        OAuthData finalData;
                        if (authentication.contains("backedCreds")
                                && authentication.getLong("expires", 0) > Calendar.getInstance()
                                .getTimeInMillis()) {
                            finalData = oAuthHelper.refreshToken(credentials,
                                    authentication.getString("backedCreds", "")); //does a request
                        } else {
                            finalData = oAuthHelper.refreshToken(credentials); //does a request
                            authentication.edit()
                                    .putLong("expires",
                                            Calendar.getInstance().getTimeInMillis() + 3000000)
                                    .apply();
                        }
                        authentication.edit()
                                .putString("backedCreds", finalData.getDataNode().toString())
                                .apply();
                        reddit.authenticate(finalData);
                        refresh = oAuthHelper.getRefreshToken();

                        Authentication.isLoggedIn = true;

                        UserSubscriptions.doCachedModSubs();

                    } catch (Exception e) {
                        e.printStackTrace();
                        if (e instanceof NetworkException) {
                            Toast.makeText(mContext, "Error " + ((NetworkException) e).getResponse()
                                            .getStatusMessage() + ": " + (e).getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }

                    }
                    didOnline = true;

                } else {
                    LogUtil.v("NOT LOGGED IN");

                    final Credentials fcreds =
                            Credentials.userlessApp(CLIENT_ID, UUID.randomUUID());
                    OAuthData authData;
                    try {

                        authData = reddit.getOAuthHelper().easyAuth(fcreds);
                        authentication.edit()
                                .putLong("expires",
                                        Calendar.getInstance().getTimeInMillis() + 3000000)
                                .apply();
                        authentication.edit()
                                .putString("backedCreds", authData.getDataNode().toString())
                                .apply();
                        reddit.authenticate(authData);

                        Authentication.name = "LOGGEDOUT";
                        Reddit.notFirst = true;
                        didOnline = true;
                        return null;

                    } catch (Exception e) {
                        e.printStackTrace();
                        if (e instanceof NetworkException) {
                            Toast.makeText(mContext, "Error " + ((NetworkException) e).getResponse()
                                            .getStatusMessage() + ": " + (e).getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }


                }
                authedOnce = true;

            } catch (Exception e) {
                //TODO fail


            }
            return null;
        }

    }

}
