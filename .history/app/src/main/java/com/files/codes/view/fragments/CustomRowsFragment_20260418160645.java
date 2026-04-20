package com.files.codes.view.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import androidx.fragment.app.FragmentActivity;
import androidx.leanback.app.RowsSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.files.codes.AppConfig;
import com.files.codes.CardPresenter;
import com.files.codes.view.fragments.testFolder.HomeNewActivity;
import com.files.codes.view.PlayerActivity;
import com.files.codes.view.VideoPlaybackActivity;
import com.files.codes.R;
import com.files.codes.database.DatabaseHelper;
import com.files.codes.database.live_tv.LiveTvList;
import com.files.codes.database.live_tv.LiveTvViewModel;
import com.files.codes.model.Channel;
import com.files.codes.model.HomeContent;
import com.files.codes.model.HomeResponse;
import com.files.codes.model.GenreWithMovies;
import com.files.codes.model.LiveTv;
import com.files.codes.model.PlaybackModel;
import com.files.codes.model.VideoContent;
import com.files.codes.model.api.ApiService;
import com.files.codes.utils.LoginAlertDialog;
import com.files.codes.utils.NetworkInst;
import com.files.codes.utils.PaidDialog;
import com.files.codes.utils.PreferenceUtils;
import com.files.codes.utils.RetrofitClient;
import com.files.codes.utils.Utils;
import com.files.codes.view.ErrorActivity;
import com.files.codes.view.presenter.LiveTvCardPresenter;
import com.files.codes.view.presenter.SliderCardPresenter;
import com.files.codes.view.presenter.TvPresenter;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class CustomRowsFragment extends RowsSupportFragment {
    private boolean loadedHomeContent;
    private boolean loadedLiveTvContent;
    //private BackgroundHelper bgHelper;
    private ArrayObjectAdapter rowsAdapter;
    private CardPresenter cardPresenter;
    private LiveTvCardPresenter liveTvCardPresenter;
    private View v;
    private FragmentActivity activity;
    private int menuPos;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activity = getActivity();
       // activity.showLogo();
        setOnItemViewClickedListener(getDefaultItemViewClickedListener());
        setOnItemViewSelectedListener(getDefaultItemSelectedListener());

        Bundle bundle = getArguments();
        if (bundle != null) {
            menuPos = bundle.getInt("menu");

            if (menuPos == 0) { // for Home content
                if (new NetworkInst(activity).isNetworkAvailable()) {
                    if (!loadedHomeContent) {
                        loadHomeContent();
                        loadedHomeContent = true;
                    }
                } else {
                    Intent intent = new Intent(activity, ErrorActivity.class);
                    startActivity(intent);
                    activity.finish();
                }

            } else if (menuPos == 3) {

                if (new NetworkInst(activity).isNetworkAvailable()) {
                    if (!loadedLiveTvContent) {
                        //loadLiveTvContent();
                        loadLiveTvLiveData();
                        loadedLiveTvContent = true;
                    }
                } else {
                    Intent intent = new Intent(activity, ErrorActivity.class);
                    startActivity(intent);
                    activity.finish();
                }

            }

        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v = super.onCreateView(inflater, container, savedInstanceState);
        return v;
    }

    private void loadChannelRows(List<LiveTv> liveTvCategories) {

        rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        liveTvCardPresenter = new LiveTvCardPresenter();

        for (int i = 0; i < liveTvCategories.size(); i++) {
            ArrayObjectAdapter listRowAdapter;
            HeaderItem header;

            listRowAdapter = new ArrayObjectAdapter(liveTvCardPresenter);
            header = new HeaderItem(i, liveTvCategories.get(i).getTitle());

            for (Channel channel : liveTvCategories.get(i).getChannels()) {
                listRowAdapter.add(channel);
            }

            rowsAdapter.add(new ListRow(header, listRowAdapter));
        }

        setAdapter(rowsAdapter);

        //setCustomPadding
        //Objects.requireNonNull(getView()).setPadding(Utils.dpToPx(-24, Objects.requireNonNull(getActivity())), Utils.dpToPx(100, getActivity()), 0, 0);
    }

    private void loadRows(List<HomeContent> homeContents) {
        rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        cardPresenter = new CardPresenter();
        SliderCardPresenter sliderCardPresenter = new SliderCardPresenter();
        TvPresenter tvPresenter = new TvPresenter();

        int i;
        for (i = 0; i < homeContents.size(); i++) {
            ArrayObjectAdapter listRowAdapter;
            HeaderItem header;
            if (i == 0) {
                // load slider
                listRowAdapter = new ArrayObjectAdapter(sliderCardPresenter);
                header = new HeaderItem(i, "");
            } else if (i == 1) {

                //load tv layout
                listRowAdapter = new ArrayObjectAdapter(tvPresenter);
                header = new HeaderItem(i, homeContents.get(i).getTitle());

            } else if (i == 2) {
                //radio content
                listRowAdapter = new ArrayObjectAdapter(tvPresenter);
                header = new HeaderItem(i, homeContents.get(i).getTitle());
            } else {
                listRowAdapter = new ArrayObjectAdapter(cardPresenter);
                header = new HeaderItem(i, homeContents.get(i).getTitle());
            }
            //for (int j = 0; j < NUM_COLS; j++) {
            for (int j = 0; j < homeContents.get(i).getContent().size(); j++) {

                VideoContent videoContent = homeContents.get(i).getContent().get(j);
                if (videoContent.getIsTvseries() != null && videoContent.getIsTvseries().equals("0")) {
                    videoContent.setType("movie");
                } else if (videoContent.getIsTvseries() != null && videoContent.getIsTvseries().equals("1")) {
                    videoContent.setType("tvseries");
                } else {
                    videoContent.setType(homeContents.get(i).getType());
                }

                listRowAdapter.add(videoContent);
            }
            rowsAdapter.add(new ListRow(header, listRowAdapter));
        }

        setAdapter(rowsAdapter);

        setCustomPadding();

    }

    public void refresh() {
        if (menuPos == 3) {
            getView().setPadding(Utils.dpToPx(-24, getContext()), Utils.dpToPx(100, getContext()), 0, 0);
        } else {
            getView().setPadding(Utils.dpToPx(-24, getContext()), Utils.dpToPx(70, getContext()), 0, 0);
        }
    }

    private void setCustomPadding() {
        getView().setPadding(Utils.dpToPx(-24, getContext()), Utils.dpToPx(100, getContext()), 0, 0);
    }

    // click listener
    private OnItemViewClickedListener getDefaultItemViewClickedListener() {
        return new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder viewHolder, Object o,
                                      RowPresenter.ViewHolder viewHolder2, Row row) {

                if (menuPos == 0) {
                    if (o == null || !(o instanceof VideoContent)) {
                        return; // Safety check
                    }
                    
                    VideoContent videoContent = (VideoContent) o;
                    
                    // Additional null checks
                    if (videoContent.getId() == null || videoContent.getType() == null) {
                        return;
                    }
                    
                    Log.d("CustomRow", videoContent.getType());

                    if (videoContent.getType().equals("tv")) {
                        PlaybackModel model = new PlaybackModel();
                        try {
                            model.setId(Long.parseLong(videoContent.getId()));
                        } catch (NumberFormatException e) {
                            model.setId(0L); // Default value if ID is not numeric
                        }
                        model.setTitle(videoContent.getTitle() != null ? videoContent.getTitle() : "");
                        model.setDescription(videoContent.getDescription() != null ? videoContent.getDescription() : "");
                        model.setVideoType(videoContent.getStreamFrom() != null ? videoContent.getStreamFrom() : "");
                        model.setCategory("tv");
                        model.setVideoUrl(videoContent.getStreamUrl() != null ? videoContent.getStreamUrl() : "");
                        model.setCardImageUrl(videoContent.getPosterUrl() != null ? videoContent.getPosterUrl() : "");
                        model.setBgImageUrl(videoContent.getThumbnailUrl() != null ? videoContent.getThumbnailUrl() : "");
                        model.setIsPaid(videoContent.getIsPaid() != null ? videoContent.getIsPaid() : "0");

                       // Intent intent = new Intent(getActivity(), PlayerActivity.class);
                       // intent.putExtra(VideoPlaybackActivity.EXTRA_VIDEO, model);
                       // startActivity(intent);


                    } else {
                       // Intent intent = new Intent(getActivity(), VideoDetailsActivity.class);
                       // intent.putExtra("id", videoContent.getId());
                       // intent.putExtra("type", videoContent.getType());
                       // intent.putExtra("thumbImage", videoContent.getThumbnailUrl());

                        //startActivity(intent);
                    }


                } else if (menuPos == 3) {
                    DatabaseHelper db = new DatabaseHelper(getContext());
                    String status = db.getActiveStatusData() != null ? db.getActiveStatusData().getStatus() : "inactive";
                    Channel channel = (Channel) o;
                    if (channel.getIsPaid().equals("1")) {
                        if (PreferenceUtils.isLoggedIn(getActivity())) {
                            if ( status.equals("active")) {
                                PlaybackModel model = new PlaybackModel();
                                model.setId(Long.parseLong(channel.getLiveTvId()));
                                model.setTitle(channel.getTvName());
                                model.setDescription(channel.getDescription());
                                model.setVideoType(channel.getStreamFrom());
                                model.setCategory("tv");
                                model.setVideoUrl(channel.getStreamUrl());
                                model.setCardImageUrl(channel.getPosterUrl());
                                model.setBgImageUrl(channel.getThumbnailUrl());
                                model.setIsPaid(channel.getIsPaid());

                                Intent intent = new Intent(getActivity(), PlayerActivity.class);
                                intent.putExtra(VideoPlaybackActivity.EXTRA_VIDEO, model);
                                startActivity(intent);
                            } else {
                                PreferenceUtils.updateSubscriptionStatus(getActivity());
                                PaidDialog dialog = new PaidDialog(getContext());
                                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                                dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
                                dialog.show();
                            }
                        }else {
                            // user is not logged in
                            // show an alert dialog
                            LoginAlertDialog dialog = new LoginAlertDialog(getActivity());
                            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                            dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
                            dialog.show();
                        }

                    } else {
                        PlaybackModel model = new PlaybackModel();
                        model.setId(Long.parseLong(channel.getLiveTvId()));
                        model.setTitle(channel.getTvName());
                        model.setDescription(channel.getDescription());
                        model.setVideoType(channel.getStreamFrom());
                        model.setCategory("tv");
                        model.setVideoUrl(channel.getStreamUrl());
                        model.setCardImageUrl(channel.getPosterUrl());
                        model.setBgImageUrl(channel.getThumbnailUrl());
                        model.setIsPaid(channel.getIsPaid());

                        Intent intent = new Intent(getActivity(), PlayerActivity.class);
                        intent.putExtra(VideoPlaybackActivity.EXTRA_VIDEO, model);
                        startActivity(intent);
                    }
                }
            }
        };
    }

    //listener for setting blur background each time when the item will select.
    protected OnItemViewSelectedListener getDefaultItemSelectedListener() {
        return new OnItemViewSelectedListener() {
            public void onItemSelected(Presenter.ViewHolder itemViewHolder, final Object item,
                                       RowPresenter.ViewHolder rowViewHolder, Row row) {

                if (item instanceof VideoContent) {
                    /*bgHelper = new BackgroundHelper(getActivity());
                    bgHelper.prepareBackgroundManager();
                    bgHelper.startBackgroundTimer(((VideoContent) item).getPosterUrl());*/
                } else if (item instanceof Channel) {
                    /*bgHelper = new BackgroundHelper(getActivity());
                    bgHelper.prepareBackgroundManager();
                    bgHelper.startBackgroundTimer(((Channel) item).getPosterUrl());*/

                }

            }
        };
    }

    public void loadHomeContent() {

        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService api = retrofit.create(ApiService.class);
        Call<HomeResponse> call = api.getHomeContent(AppConfig.API_KEY);
        call.enqueue(new Callback<HomeResponse>() {
            @Override
            public void onResponse(Call<HomeResponse> call, Response<HomeResponse> response) {

                if (response.isSuccessful()) {
                    // Convert HomeResponse to List<HomeContent> for compatibility
                    List<HomeContent> homeContents = convertHomeResponseToList(response.body());
                    //Log.d("size:", homeContents.size()+"");

                    if (homeContents.size() > 0) {
                        loadRows(homeContents);
                    } else {
                        Toast.makeText(getContext(), getResources().getString(R.string.no_data_found), Toast.LENGTH_SHORT).show();
                    }


                } else {
                    Toast.makeText(getContext(), response.errorBody().toString(), Toast.LENGTH_SHORT).show();
                }

                 }

            @Override
            public void onFailure(Call<HomeResponse> call, Throwable t) {
                t.printStackTrace();
                Toast.makeText(getContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
                 }
        });


    }

    private void loadLiveTvLiveData() {

        LiveTvViewModel viewModel = new ViewModelProvider(getActivity()).get(LiveTvViewModel.class);
        viewModel.getLiveTvListLiveData().observe(getActivity(), new Observer<LiveTvList>() {
            @Override
            public void onChanged(LiveTvList liveTvList) {
                if (liveTvList != null){
                    List<LiveTv> liveTvCategories = liveTvList.getLiveTvList();
                    if (liveTvCategories.size() > 0) {
                        loadChannelRows(liveTvCategories);
                    } else {
                        Toast.makeText(activity, getResources().getString(R.string.no_data_found), Toast.LENGTH_SHORT).show();
                    }
                }
                   }
        });
    }

    private List<HomeContent> convertHomeResponseToList(HomeResponse response) {
        List<HomeContent> homeContents = new ArrayList<>();
        
        // Add Genre Featured from features_genre_and_movie
        if (response.getFeaturesGenreAndMovie() != null && !response.getFeaturesGenreAndMovie().isEmpty()) {
            HomeContent genreFeatured = new HomeContent();
            genreFeatured.setId("genre_featured");
            genreFeatured.setType("features_genre_and_movie");
            genreFeatured.setTitle(getString(R.string.featured_genre));
            genreFeatured.setDescription(getString(R.string.featured_description));
            
            // Extract movies from first genre (F)
            GenreWithMovies firstGenre = response.getFeaturesGenreAndMovie().get(0);
            if (firstGenre != null && firstGenre.getVideos() != null && !firstGenre.getVideos().isEmpty()) {
                genreFeatured.setContent(firstGenre.getVideos());
            } else {
                // Fallback: Use latest movies if genre videos are empty
                if (response.getLatestMovies() != null && !response.getLatestMovies().isEmpty()) {
                    List<VideoContent> featuredMovies = response.getLatestMovies().subList(0, 
                        Math.min(10, response.getLatestMovies().size()));
                    genreFeatured.setContent(featuredMovies);
                }
            }
            
            homeContents.add(genreFeatured);
        }
        
        // Add Latest Movies
        if (response.getLatestMovies() != null) {
            HomeContent movies = new HomeContent();
            movies.setId("2");
            movies.setType("movie");
            movies.setTitle(getString(R.string.latest_movie));
            movies.setContent(response.getLatestMovies());
            homeContents.add(movies);
        }
        
        // Add Latest TV Series
        if (response.getLatestTvseries() != null) {
            HomeContent tvseries = new HomeContent();
            tvseries.setId("3");
            tvseries.setType("tvseries");
            tvseries.setTitle(getString(R.string.latest_tv_series));
            tvseries.setContent(response.getLatestTvseries());
            homeContents.add(tvseries);
        }
        
        return homeContents;
    }
}
