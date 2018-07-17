package com.tr.feelingme;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

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
    private TwitterLoginButton twitterLoginButton;
    private TwitterAuthClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        showBusy(true);

        connectTwitter();
//        connectSpotify();
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
        } else {
            twitterLoginButton.onActivityResult(requestCode, resultCode, intent);
        }
    }

    public void onPlay(View view) {
        if (spotifyApi == null)
            return;

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://9u0w997c35.execute-api.us-east-1.amazonaws.com/dev/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        FeelingMe feelingMe = retrofit.create(FeelingMe.class);
        feelingMe.search("Men, we can no longer avert our eyes from what is deeply broken in ourselves and in others, writes Mark Greene. We can no longer cater to our discomfort, avoiding at all costs the challenging conversations required of us.")
                .enqueue(new retrofit2.Callback<List<List<String>>>() {
                    @Override
                    public void onResponse(Call<List<List<String>>> call,
                                           retrofit2.Response<List<List<String>>> response) {
                        List<List<String>> result = response.body();

                        if (result.size() > 0) {
                            SpotifyService spotify = spotifyApi.getService();
                            spotify.searchPlaylists(result.get(0).get(1), new Callback<PlaylistsPager>() {
                                @Override
                                public void success(PlaylistsPager playlistsPager, Response response) {
                                    String uri = playlistsPager.playlists.items.get(0).uri;
                                    Intent launcher = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                                    startActivity(launcher);
                                }

                                @Override
                                public void failure(RetrofitError error) {
                                }
                            });
                        }
                    }

                    @Override
                    public void onFailure(Call<List<List<String>>> call, Throwable t) {
                    }
                });
    }

    private void connectSpotify() {
        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID,
                AuthenticationResponse.Type.TOKEN, REDIRECT_URI);
        builder.setScopes(new String[]{ "user-read-private" });
        AuthenticationRequest request = builder.build();
        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    private void showBusy(boolean isBusy) {
        View busyView = findViewById(R.id.busyIndicator);
        busyView.setVisibility(isBusy ? View.VISIBLE : View.GONE);
    }

    private void connectTwitter() {
        TwitterConfig config = new TwitterConfig.Builder(this)
                .twitterAuthConfig(new TwitterAuthConfig(TWITTER_KEY, SECRET))
                .debug(true)
                .build();
        Twitter.initialize(config);

        client = new TwitterAuthClient();
        TwitterSession session = TwitterCore.getInstance().getSessionManager().getActiveSession();

        if (session == null) {
            //if user is not authenticated start authenticating
            client.authorize(this, new com.twitter.sdk.android.core.Callback<TwitterSession>() {
                @Override
                public void success(Result<TwitterSession> result) {

                    TwitterSession twitterSession = result.data;
                    getLastTweetMessage(twitterSession);
                }

                @Override
                public void failure(TwitterException e) {
                    // Do something on failure
                }
            });
        } else {
            // already authenticated
            getLastTweetMessage(session);
        }
    }

    private void getLastTweetMessage(TwitterSession session) {
        TwitterApiClient twitterApiClient = TwitterCore.getInstance().getApiClient();
        Call<List<Tweet>> call = twitterApiClient.getStatusesService().userTimeline( session.getUserId(), null, 1, null, null, true, true, true, false);
        call.enqueue(new com.twitter.sdk.android.core.Callback<List<Tweet>>() {
            @Override
            public void success(Result<List<Tweet>> result) {
                //Do something with result
                if (!result.data.isEmpty()) {
                    Tweet tweet = result.data.get(0);
                }
            }

            public void failure(TwitterException exception) {
                //Do something on failure
            }
        });
    }
}
