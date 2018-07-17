package com.tr.feelingme;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.widget.Toast;
import android.util.Log;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.PlaylistsPager;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterConfig;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterAuthClient;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;
import com.twitter.sdk.android.core.TwitterApiClient;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.models.Tweet;

import static com.spotify.sdk.android.authentication.LoginActivity.REQUEST_CODE;

public class MainActivity extends AppCompatActivity {

    private static final String CLIENT_ID = "01005ab3eacb4b5a9644497c043f4d24";
    private static final String REDIRECT_URI = "yourcustomprotocol://callback";
    private static final String TWITTER_KEY = "vfex6InKBJhKu7wf38gKriV2q";
    private static final String SECRET = "rVlfIGZf8d08G47BpJlfM3a4wM7Ot9UE68dY2bDRM3MvKPVJSQ";

    private SpotifyApi spotifyApi;
    private TweetsPagerAdapter tweetsAdapter;
    private TwitterAuthClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        //| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        //| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);

        tweetsAdapter = new TweetsPagerAdapter(this);
        ViewPager pager = (ViewPager)findViewById(R.id.pager);
        pager.setAdapter(tweetsAdapter);

        showBusy(true);
        showPlayButton(true);

        connectTwitter();
        connectSpotify();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                spotifyApi = new SpotifyApi();
                spotifyApi.setAccessToken(response.getAccessToken());
            }
            showBusy(false);

//            tweetsAdapter.setTweets(new String[] {
//                    "Just between us, I have a teleportation stargate to the Andromeda galaxy. It’s amazing",
//                    "Men, we can no longer avert our eyes from what is deeply broken in ourselves and in others, writes Mark Greene. We can no longer cater to our discomfort, avoiding at all costs the challenging conversations required of us.",
//                    "Capturing the WorldCupFinal Reuters photographer KPfaffenbach captures a heavy downpour as FIFA president Infantino is joined by Putin, Macron and Grabar-Kitarovic during the presentation"
//            });
        } else {
            if (client != null) {
                client.onActivityResult(requestCode, resultCode, intent);
            }
        }
    }

    public void onPlay(View view) {
        if (spotifyApi == null)
            return;

        showPlayButton(false);

        Toast.makeText(MainActivity.this,
                "Searching",
                Toast.LENGTH_SHORT).show();

        ViewPager pager = (ViewPager)findViewById(R.id.pager);
        String tweet = tweetsAdapter.getTweets()[pager.getCurrentItem()];

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://9u0w997c35.execute-api.us-east-1.amazonaws.com/dev/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        FeelingMe feelingMe = retrofit.create(FeelingMe.class);
        feelingMe.search(tweet)
                .enqueue(new retrofit2.Callback<List<List<String>>>() {
                    @Override
                    public void onResponse(Call<List<List<String>>> call,
                                           retrofit2.Response<List<List<String>>> response) {
                        List<List<String>> result = response.body();

                        if (result != null && result.size() > 0 && result.get(0) != null
                                && result.get(0).size() > 1 && result.get(0).get(1) != null) {
                            SpotifyService spotify = spotifyApi.getService();
                            spotify.searchPlaylists(result.get(0).get(1), new Callback<PlaylistsPager>() {
                                @Override
                                public void success(PlaylistsPager playlistsPager, Response response) {
                                    if (playlistsPager.playlists.items.size() > 0) {
                                        String uri = playlistsPager.playlists.items.get(0).uri;
                                        Intent launcher = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                                        startActivity(launcher);
                                    } else {
                                        Toast.makeText(MainActivity.this,
                                                "No playlist found",
                                                Toast.LENGTH_LONG).show();
                                    }
                                }

                                @Override
                                public void failure(RetrofitError error) {
                                }
                            });
                        } else {
                            Toast.makeText(MainActivity.this,
                                    "No playlist found",
                                    Toast.LENGTH_LONG).show();
                        }

                        showPlayButton(true);
                    }

                    @Override
                    public void onFailure(Call<List<List<String>>> call, Throwable t) {
                        showPlayButton(true);
                    }
                });
    }

    private void connectSpotify() {
        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID,
                AuthenticationResponse.Type.TOKEN, REDIRECT_URI);
        builder.setScopes(new String[]{ });
        AuthenticationRequest request = builder.build();
        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    private void showBusy(boolean isBusy) {
        View busyView = findViewById(R.id.busyIndicator);
        busyView.setVisibility(isBusy ? View.VISIBLE : View.GONE);
    }

    private void showPlayButton(boolean isVisible) {
        if (isVisible) {
            float dp = getResources().getDisplayMetrics().density;
            View button = findViewById(R.id.playButton);
            ResizeAnimation resizeAnimation = new ResizeAnimation(
                    button,
                    (int)(10 * dp),
                    (int)(128 * dp)
            );
            resizeAnimation.setDuration(1000);
            resizeAnimation.setRepeatCount(Animation.INFINITE);
            button.startAnimation(resizeAnimation);
        } else {
            // View button = findViewById(R.id.playButton);
            // button.clearAnimation();
        }
    }

    private void connectTwitter() {
        Log.w("FeelingMe", "connectTwitter");
        TwitterConfig config = new TwitterConfig.Builder(this)
                .twitterAuthConfig(new TwitterAuthConfig(TWITTER_KEY, SECRET))
                .debug(true)
                .build();
        Twitter.initialize(config);

        client = new TwitterAuthClient();

        TwitterSession session = TwitterCore.getInstance().getSessionManager().getActiveSession();

        if (session == null) {
            //if user is not authenticated start authenticating
            Log.w("FeelingMe", "session is null. Request authorize ...");
            client.authorize(this, new com.twitter.sdk.android.core.Callback<TwitterSession>() {
                @Override
                public void success(Result<TwitterSession> result) {
                    Log.w("FeelingMe", "onauthorize success ...");
                    TwitterSession twitterSession = result.data;
                    getLastTweetMessage(twitterSession);
                }

                @Override
                public void failure(TwitterException e) {
                    Log.w("FeelingMe", "onauthorize failure ...");
                    // Do something on failure
                }
            });
        } else {
            // already authenticated
            getLastTweetMessage(session);
        }
    }

    private void getLastTweetMessage(TwitterSession session) {
        Log.w("FeelingMe", "getLastTweetMessage ...");
        TwitterApiClient twitterApiClient = TwitterCore.getInstance().getApiClient();
        Call<List<Tweet>> call = twitterApiClient.getStatusesService().userTimeline( session.getUserId(), null, 10, null, null, true, true, true, false);
        call.enqueue(new com.twitter.sdk.android.core.Callback<List<Tweet>>() {
            @Override
            public void success(Result<List<Tweet>> result) {
                //Do something with result
                Log.w("FeelingMe", "onuserTimeline success ...");

                int size = result.data.size();
                String[] msgs = new String[size];
                for (int i=0; i<size; i++) {
                    Tweet tweet = result.data.get(i);
                    msgs[i] = tweet.text;
                }
                tweetsAdapter.setTweets(msgs);
            }

            public void failure(TwitterException exception) {
                //Do something on failure
                Log.w("FeelingMe", "onuserTimeline failure ...");
            }
        });
    }
}
