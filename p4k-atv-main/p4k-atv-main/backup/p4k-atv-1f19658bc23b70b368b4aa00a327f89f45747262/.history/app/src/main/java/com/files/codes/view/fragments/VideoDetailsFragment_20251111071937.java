package com.files.codes.view.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.leanback.app.DetailsSupportFragment;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.DetailsOverviewLogoPresenter;
import androidx.leanback.widget.DetailsOverviewRow;
import androidx.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import androidx.leanback.widget.FullWidthDetailsOverviewSharedElementHelper;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnActionClickedListener;
import androidx.leanback.widget.SparseArrayObjectAdapter;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.files.codes.AppConfig;
import com.files.codes.R;
import com.files.codes.database.DatabaseHelper;
import com.files.codes.model.FavoriteModel;
import com.files.codes.model.PlaybackModel;
import com.files.codes.model.Video;
import com.files.codes.model.VideoContent;
import com.files.codes.model.api.ApiService;
import com.files.codes.model.movieDetails.Episode;
import com.files.codes.model.movieDetails.Episode;
import com.files.codes.model.movieDetails.MovieSingleDetails;
import com.files.codes.model.movieDetails.RelatedMovie;
import com.files.codes.model.movieDetails.Season;
import com.files.codes.utils.BackgroundHelper;
import com.files.codes.utils.LoginAlertDialog;
import com.files.codes.utils.PaidDialog;
import com.files.codes.utils.PaletteColors;
import com.files.codes.utils.PreferenceUtils;
import com.files.codes.utils.RetrofitClient;
import com.files.codes.utils.ToastMsg;
import com.files.codes.utils.Utils;
import com.files.codes.utils.Phim4kClient;
import com.files.codes.model.phim4k.Phim4kEpisodeServer;
import com.files.codes.model.phim4k.Phim4kEpisode;
import com.files.codes.model.phim4k.Phim4kMovie;
import com.files.codes.view.PlayerActivity;
import com.files.codes.view.VideoPlaybackActivity;
import com.files.codes.view.adapter.ServerAdapter;
import com.files.codes.view.presenter.ActionButtonPresenter;
import com.files.codes.view.presenter.CustomMovieDetailsPresenter;
import com.files.codes.view.presenter.EpisodPresenter;
import com.files.codes.view.presenter.FullWidthEpisodePresenter;
import com.files.codes.view.presenter.HeroStyleMovieDetailsPresenter;
import com.files.codes.view.presenter.MovieDetailsDescriptionPresenter;
import com.files.codes.view.presenter.PaginationPresenter;
import com.files.codes.view.presenter.RelatedPresenter;
import com.files.codes.view.presenter.SeasonSelectorPresenter;
import com.files.codes.model.movieDetails.PaginationItem;
import com.files.codes.model.movieDetails.SeasonSelectorItem;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class VideoDetailsFragment extends DetailsSupportFragment implements Palette.PaletteAsyncListener{
    private final String TAG = "VideoDetailsFragment";
    public static String TRANSITION_NAME = "poster_transition";
    private FullWidthDetailsOverviewRowPresenter mFullWidthMovieDetailsPresenter;
    private ArrayObjectAdapter mAdapter;

    private DetailsOverviewRow mDetailsOverviewRow;
    public static MovieSingleDetails movieDetails = null;
    private Context mContext;
    private String type;
    private String id;
    private String thumbUrl;
    private BackgroundHelper bgHelper;

    private static final int ACTION_PLAY = 1;
    private static final int ACTION_WATCH_LATER = 2;
    private boolean favStatus;
    private String userId = "";
    private String isPaid = "";
    
    // TV Series pagination state
    private List<Season> allSeasons;
    private int currentSeasonIndex = 0;
    private int currentPage = 0;
    private static final int EPISODES_PER_PAGE = 10;
    private ListRow seasonSelectorRow;
    private ListRow episodesRow;
    private ListRow paginationRow;
    private SeasonSelectorPresenter seasonSelectorPresenter; // Store presenter reference

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext();
        type = getActivity().getIntent().getStringExtra("type");
        id = getActivity().getIntent().getStringExtra("id");
        thumbUrl = getActivity().getIntent().getStringExtra("thumbImage");
        //get userId from DB
        //check mandatory login is enabled or not
        //and also check user logged in or not
        if (PreferenceUtils.isLoggedIn(getContext())) {
            this.userId = PreferenceUtils.getUserId(getContext());
            PreferenceUtils.updateSubscriptionStatus(getContext());
        }else {
            //set a static user id to avoid app crashing
            this.userId = "1";
        }

        bgHelper = new BackgroundHelper(getActivity());
        bgHelper.prepareBackgroundManager();
        bgHelper.updateBackground(thumbUrl);

        setUpAdapter();
        setUpDetailsOverviewRow();
    }


    private void setUpAdapter() {
        // Create the HeroStyle presenter and set up listeners
        HeroStyleMovieDetailsPresenter heroPresenter = new HeroStyleMovieDetailsPresenter();
        setupHeroStyleListeners(heroPresenter);
        
        // Create the FullWidthPresenter with Hero-style layout for movies
        mFullWidthMovieDetailsPresenter = new CustomMovieDetailsPresenter(heroPresenter,
                new DetailsOverviewLogoPresenter());

        //mFullWidthMovieDetailsPresenter.setActionsBackgroundColor(ContextCompat.getColor(getActivity(), R.color.black_30));
        mFullWidthMovieDetailsPresenter.setBackgroundColor(ContextCompat.getColor(getActivity(), android.R.color.transparent));

        // Handle the transition, the Helper is mainly used because the ActivityTransition is being passed from
        // The Activity into the Fragment
        FullWidthDetailsOverviewSharedElementHelper helper = new FullWidthDetailsOverviewSharedElementHelper();
        helper.setSharedElementEnterTransition(getActivity(), TRANSITION_NAME); // the transition name is important
        mFullWidthMovieDetailsPresenter.setListener(helper); // Attach the listener
        // Define if this element is participating in the transition or not
        mFullWidthMovieDetailsPresenter.setParticipatingEntranceTransition(false);

        // Class presenter selector allows the Adapter to render Rows and the details
        // It can be used in any of the Adapters by the Leanback library
        ClassPresenterSelector classPresenterSelector = new ClassPresenterSelector();
        classPresenterSelector.addClassPresenter(DetailsOverviewRow.class, mFullWidthMovieDetailsPresenter);
        classPresenterSelector.addClassPresenter(ListRow.class, new ListRowPresenter());
        mAdapter = new ArrayObjectAdapter(classPresenterSelector);

        // Sets the adapter to the fragment
        setAdapter(mAdapter);


    }

    private void setupHeroStyleListeners(HeroStyleMovieDetailsPresenter heroPresenter) {
        // Set up Play button listener
        heroPresenter.setOnPlayClickListener(movie -> {
            Log.d(TAG, "Play button clicked for movie: " + movie.getTitle());
            playMovie(movie);
        });
        
        // Set up Favorite button listener
        heroPresenter.setOnFavoriteClickListener(movie -> {
            Log.d(TAG, "Favorite button clicked for movie: " + movie.getTitle());
            toggleFavorite(movie);
        });
    }

    private void playMovie(MovieSingleDetails movie) {
        // Reuse existing play logic from the fragment
        if (movieDetails != null && movieDetails.getVideos() != null && !movieDetails.getVideos().isEmpty()) {
            // Start playing the movie
            Intent intent = new Intent(getActivity(), PlayerActivity.class);
            intent.putExtra("id", movieDetails.getVideosId());
            intent.putExtra("type", movieDetails.getType() != null ? movieDetails.getType() : "movie");
            startActivity(intent);
        }
    }

    private void toggleFavorite(MovieSingleDetails movie) {
        // Add to favorites logic
        DatabaseHelper databaseHelper = new DatabaseHelper(getContext());
        if (movie != null) {
            FavoriteModel favoriteModel = new FavoriteModel();
            favoriteModel.setVideoId(movie.getVideosId());
            favoriteModel.setTitle(movie.getTitle());
            favoriteModel.setPosterUrl(movie.getPosterUrl());
            favoriteModel.setVideoType(movie.getType());
            
            if (databaseHelper.isFavoriteExists(favoriteModel.getVideoId())) {
                databaseHelper.deleteFavorite(favoriteModel.getVideoId());
                ToastMsg.toastIconSuccess(getActivity(), "Removed from favorites", R.drawable.ic_heart_fill);
            } else {
                databaseHelper.addToFavorite(favoriteModel);
                ToastMsg.toastIconSuccess(getActivity(), "Added to favorites", R.drawable.ic_heart_fill);
            }
        }
    }

    private void setUpDetailsOverviewRow() {
        if (getActivity() == null || !isAdded()) {
            Log.w(TAG, "setUpDetailsOverviewRow: Fragment not attached, skipping setup");
            return;
        }
        
        try {
            mDetailsOverviewRow = new DetailsOverviewRow(new MovieSingleDetails());
            if (mAdapter != null) {
                mAdapter.add(mDetailsOverviewRow);
            }
            loadImage(thumbUrl);

            // Check if this is a phim4k movie/series
            if (id != null && id.startsWith("phim4k_")) {
                Log.e(TAG, "setUpDetailsOverviewRow: Detected phim4k content, ID: " + id);
                getPhim4kData(type, id);
                getFavStatus();
            } else if (type != null && type.equals("movie")) {
                // fetch movie details
                Log.e(TAG, "setUpDetailsOverviewRow: Loading movie data for ID: " + id);
                getData(type, id);
                getFavStatus();

            } else if (type != null && type.equals("tvseries")) {
                // fetch tv series details
                Log.e(TAG, "setUpDetailsOverviewRow: Loading TV series data for ID: " + id);
                getTvSeries(type, id);
                getFavStatus();
            } else {
                Log.w(TAG, "setUpDetailsOverviewRow: Unknown type: " + type);
            }
        } catch (Exception e) {
            Log.e(TAG, "setUpDetailsOverviewRow: Error setting up details overview", e);
        }
    }

    public void setActionAdapter(boolean favAdded) {
        if (type.equals("movie")) {
            setMovieActionAdapter(favAdded);
        } else if (type.equals("tvseries")) {
            setTvSeriesActionAdapter(favAdded);
        }

    }

    public void setMovieActionAdapter(final boolean favAdded) {
        final SparseArrayObjectAdapter adapter = new SparseArrayObjectAdapter(new ActionButtonPresenter());
        //set play button text
        //if user has subscription, button text will be "play now"
        DatabaseHelper db = new DatabaseHelper(getContext());
        final String status = db.getActiveStatusData() != null ? db.getActiveStatusData().getStatus() : "inactive";
        
        // Safe check for isPaid - default to "0" if null
        String paidStatus = isPaid != null ? isPaid : "0";
        
        if (paidStatus.equals("1")) {
            if (status.equals("active")) {
                adapter.set(ACTION_PLAY, new Action(ACTION_PLAY, getResources().getString(R.string.play_now)));
            } else {
                adapter.set(ACTION_PLAY, new Action(ACTION_PLAY, getResources().getString(R.string.go_premium)));
            }
        } else {
            adapter.set(ACTION_PLAY, new Action(ACTION_PLAY, getResources().getString(R.string.play_now)));
        }

        if (favAdded) {
            adapter.set(ACTION_WATCH_LATER, new Action(ACTION_WATCH_LATER, getResources().getString(R.string.remove_from_fav)));
        } else {
            adapter.set(ACTION_WATCH_LATER, new Action(ACTION_WATCH_LATER, getResources().getString(R.string.add_to_fav)));
        }

        mDetailsOverviewRow.setActionsAdapter(adapter);


        mFullWidthMovieDetailsPresenter.setOnActionClickedListener(new OnActionClickedListener() {
            @Override
            public void onActionClicked(Action action) {
                if (action.getId() == 1) {
                    PreferenceUtils.clearSubscriptionSavedData(getActivity());
                    if (movieDetails != null) {
                        String isPaid = movieDetails.getIsPaid() != null ? movieDetails.getIsPaid() : "0";
                        if (isPaid.equals("1")) {
                            //check user is logged in or not
                            if (PreferenceUtils.isLoggedIn(getActivity())) {
                                if (status.equals("active")) {
                                    openServerDialog(movieDetails.getVideos());
                                } else {
                                    //subscription is not active
                                    //new PaidDialog(getActivity()).showPaidContentAlertDialog();
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
                            openServerDialog(movieDetails.getVideos());
                        }
                    }
                } else {

                    if (favStatus) {
                        // remove from fav
                        removeFromFav();
                    } else {
                        // add to fav
                        addToFav();
                    }

                }
            }
        });
    }

    public void setTvSeriesActionAdapter(boolean favAdded) {
        SparseArrayObjectAdapter adapter = new SparseArrayObjectAdapter(new ActionButtonPresenter());
        if (favAdded) {
            adapter.set(ACTION_WATCH_LATER, new Action(ACTION_WATCH_LATER, getResources().getString(R.string.remove_from_fav)));
        } else {
            adapter.set(ACTION_WATCH_LATER, new Action(ACTION_WATCH_LATER, getResources().getString(R.string.add_to_fav)));
        }

        mDetailsOverviewRow.setActionsAdapter(adapter);

        mFullWidthMovieDetailsPresenter.setOnActionClickedListener(new OnActionClickedListener() {
            @Override
            public void onActionClicked(Action action) {
                if (favStatus) {
                    // remove from fav
                    removeFromFav();
                } else {
                    // add to fav
                    addToFav();
                }
            }
        });
    }

    private void loadImage(String url) {
        /*mDetailsOverviewRow.setImageDrawable(getContext().getResources().
                getDrawable(R.drawable.logo));*/
        
        // Check if URL is valid before loading with Picasso
        if (url != null && !url.trim().isEmpty()) {
            Picasso.get()
                    .load(url)
                    .resize(300, 500)
                    .centerCrop()
                    .placeholder(R.drawable.poster_placeholder)
                    .into(new PicassoImageCardViewTarget());
        } else {
            // Load placeholder directly if URL is empty
            mDetailsOverviewRow.setImageDrawable(getContext().getResources().
                    getDrawable(R.drawable.poster_placeholder));
        }
    }

    private void bindMovieDetails(MovieSingleDetails singleDetails) {
        if (singleDetails == null) {
            Log.e(TAG, "bindMovieDetails: singleDetails is null, cannot bind details");
            return;
        }
        
        if (mDetailsOverviewRow == null) {
            Log.e(TAG, "bindMovieDetails: mDetailsOverviewRow is null, creating new one");
            mDetailsOverviewRow = new DetailsOverviewRow(singleDetails);
            if (mAdapter != null) {
                mAdapter.add(mDetailsOverviewRow);
            }
        }
        
        if (getActivity() == null || getContext() == null) {
            Log.e(TAG, "bindMovieDetails: Activity or Context is null, cannot bind");
            return;
        }
        
        movieDetails = singleDetails;
        // Bind the details to the row
        mDetailsOverviewRow.setItem(movieDetails);
        loadImage(thumbUrl);
        
        Log.e(TAG, "bindMovieDetails: Successfully bound details for " + movieDetails.getTitle());
    }

    private void changePalette(Bitmap bmp) {
        Palette.from(bmp).generate(this);
    }

    @Override
    public void onGenerated(Palette palette) {
        PaletteColors colors = Utils.getPaletteColors(palette);
        mFullWidthMovieDetailsPresenter.setActionsBackgroundColor(colors.getStatusBarColor());
        mFullWidthMovieDetailsPresenter.setBackgroundColor(colors.getToolbarBackgroundColor());
    }

    class PicassoImageCardViewTarget implements Target {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            mDetailsOverviewRow.setImageBitmap(getContext(), bitmap);
        }

        @Override
        public void onBitmapFailed(Exception e, Drawable errorDrawable) {

        }


        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {

        }
    }

    public static int dpToPx(int dp, Context ctx) {
        float density = ctx.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    public void openServerDialog(final List<Video> videos) {
        if (videos.size() != 0) {
            List<Video> videoList = new ArrayList<>();
            videoList.clear();

            for (Video video : videos) {
                if (video.getFileType() != null && !video.getFileType().equalsIgnoreCase("embed")) {
                    videoList.add(video);
                }
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            View view = LayoutInflater.from(getActivity()).inflate(R.layout.layout_server_tv, null);
            RecyclerView serverRv = view.findViewById(R.id.serverRv);
            ServerAdapter serverAdapter = new ServerAdapter(getActivity(), videoList, "movie");
            serverRv.setLayoutManager(new LinearLayoutManager(getActivity()));
            serverRv.setHasFixedSize(true);
            serverRv.setAdapter(serverAdapter);

            Button closeBt = view.findViewById(R.id.close_bt);

            builder.setView(view);

            final AlertDialog dialog = builder.create();
            dialog.show();

            closeBt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });

            final ServerAdapter.OriginalViewHolder[] viewHolder = {null};
            serverAdapter.setOnItemClickListener(new ServerAdapter.OnItemClickListener() {

                @Override
                public void onItemClick(View view, Video obj, int position, ServerAdapter.OriginalViewHolder holder) {
                    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity())
                            .toBundle();

                    PlaybackModel video = new PlaybackModel();
                    
                    // Set movieId for watch history tracking
                    video.setMovieId(id);
                    
                    // Handle phim4k IDs differently since they're not numeric
                    if (id.startsWith("phim4k_")) {
                        // For phim4k content, use hash code as ID
                        video.setId((long) id.hashCode());
                    } else {
                        // For regular content, parse as Long
                        try {
                            video.setId(Long.parseLong(id));
                        } catch (NumberFormatException e) {
                            Log.e("VideoDetailsFragment", "Failed to parse ID as Long: " + id + ", using hashCode");
                            video.setId((long) id.hashCode());
                        }
                    }
                    
                    // Set complete movie metadata for watch history
                    video.setTitle(movieDetails.getTitle());
                    video.setDescription(movieDetails.getDescription());
                    video.setCategory("movie");
                    video.setVideo(obj);
                    ArrayList<Video> videoListForIntent = new ArrayList<>(videoList);
                    video.setVideoList(videoListForIntent);
                    video.setVideoUrl(obj.getFileUrl());
                    video.setVideoType(obj.getFileType());
                    video.setBgImageUrl(movieDetails.getPosterUrl());
                    video.setCardImageUrl(movieDetails.getThumbnailUrl());
                    video.setIsPaid(movieDetails.getIsPaid());
                    
                    // Log movie details for debugging
                    Log.e("VideoDetailsFragment", "Movie ID: " + id);
                    Log.e("VideoDetailsFragment", "Title: " + movieDetails.getTitle());
                    Log.e("VideoDetailsFragment", "Description: " + (movieDetails.getDescription() != null ? movieDetails.getDescription() : "NULL"));
                    Log.e("VideoDetailsFragment", "Release: " + (movieDetails.getRelease() != null ? movieDetails.getRelease() : "NULL"));
                    Log.e("VideoDetailsFragment", "Runtime: " + (movieDetails.getRuntime() != null ? movieDetails.getRuntime() : "NULL"));
                    Log.e("VideoDetailsFragment", "VideoQuality: " + (movieDetails.getVideoQuality() != null ? movieDetails.getVideoQuality() : "NULL"));
                    Log.e("VideoDetailsFragment", "IsTvSeries: " + (movieDetails.getIsTvseries() != null ? movieDetails.getIsTvseries() : "NULL"));
                    
                    // Set available metadata for watch history
                    if (movieDetails.getRelease() != null) {
                        video.setReleaseDate(movieDetails.getRelease());
                    }
                    if (movieDetails.getRuntime() != null) {
                        video.setRuntime(movieDetails.getRuntime());
                    }
                    if (movieDetails.getVideoQuality() != null) {
                        video.setVideoQuality(movieDetails.getVideoQuality());
                    }
                    video.setIsTvSeries(movieDetails.getIsTvseries());
                    
                    // Set genre info if available
                    if (movieDetails.getGenre() != null) {
                        video.setGenreList(movieDetails.getGenre());
                    }

                    Intent playerIntent = new Intent(getActivity(), PlayerActivity.class);
                    playerIntent.putExtra(VideoPlaybackActivity.EXTRA_VIDEO, video);
                    startActivity(playerIntent);
                    dialog.dismiss();
                }
            });
        }else {
            new ToastMsg(getContext()).toastIconError("No video available.");
        }

    }

    private void getData(String vtype, final String vId) {

        final SpinnerFragment spinnerFragment = new SpinnerFragment();
        final FragmentManager fm = getFragmentManager();
        fm.beginTransaction().add(R.id.details_fragment, spinnerFragment).commit();

        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService api = retrofit.create(ApiService.class);
        Call<MovieSingleDetails> call = api.getSingleDetail(AppConfig.API_KEY, vtype, vId);
        call.enqueue(new Callback<MovieSingleDetails>() {
            @Override
            public void onResponse(Call<MovieSingleDetails> call, Response<MovieSingleDetails> response) {
                if (getActivity() == null || !isAdded()) {
                    Log.w(TAG, "getData onResponse: Fragment not attached, skipping response handling");
                    return;
                }
                
                try {
                    fm.beginTransaction().remove(spinnerFragment).commitAllowingStateLoss();
                } catch (Exception e) {
                    Log.w(TAG, "getData onResponse: Error removing spinner fragment", e);
                }
                
                if (response.code() == 200 && response.body() != null) {
                    MovieSingleDetails singleDetails = response.body();
                    singleDetails.setType("movie");
                    isPaid = singleDetails.getIsPaid() != null ? singleDetails.getIsPaid() : "0";
                    
                    setMovieActionAdapter(favStatus);
                    bindMovieDetails(singleDetails);
                    Log.e(TAG, "getData onResponse: Successfully loaded movie details for " + singleDetails.getTitle());

                    //new DetailRowBuilderTask().execute(singleDetails);
                    String[] subcategories = {
                            getString(R.string.you_may_also_like)
                    };

                    ArrayObjectAdapter rowAdapter = new ArrayObjectAdapter(new RelatedPresenter(getActivity()));
                    if (response.body().getRelatedMovie() != null) {
                        for (RelatedMovie model : response.body().getRelatedMovie()) {
                            model.setType("movie");
                            rowAdapter.add(model);
                        }
                    }
                    HeaderItem header = new HeaderItem(0, subcategories[0]);
                    if (mAdapter != null) {
                        mAdapter.add(new ListRow(header, rowAdapter));
                    }
                } else {
                    Log.e(TAG, "getData onResponse: Failed to load data - Response code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<MovieSingleDetails> call, Throwable t) {
                if (getActivity() == null || !isAdded()) {
                    return;
                }
                
                Log.e(TAG, "getData onFailure: Failed to load movie data", t);
                try {
                    fm.beginTransaction().remove(spinnerFragment).commitAllowingStateLoss();
                } catch (Exception e) {
                    Log.w(TAG, "getData onFailure: Error removing spinner fragment", e);
                }
            }
        });

    }

    private void getTvSeries(String vtype, final String vId) {

        final SpinnerFragment spinnerFragment = new SpinnerFragment();
        final FragmentManager fm = getFragmentManager();
        fm.beginTransaction().add(R.id.details_fragment, spinnerFragment).commit();

        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService api = retrofit.create(ApiService.class);
        Call<MovieSingleDetails> call = api.getSingleDetail(AppConfig.API_KEY, vtype, vId);
        call.enqueue(new Callback<MovieSingleDetails>() {
            @Override
            public void onResponse(Call<MovieSingleDetails> call, Response<MovieSingleDetails> response) {
                if (response.code() == 200) {
                    MovieSingleDetails singleDetails = new MovieSingleDetails();
                    singleDetails = response.body();
                    singleDetails.setType("tvseries");
                    isPaid = response.body().getIsPaid();
                    setTvSeriesActionAdapter(favStatus);
                    bindMovieDetails(response.body());

                    // Store all seasons
                    allSeasons = new ArrayList<Season>();
                    if (response.body().getSeason() != null) {
                        allSeasons.addAll(response.body().getSeason());
                    }

                    if (allSeasons.size() == 0) {
                        Toast.makeText(mContext, "Seasons are not found. :(", Toast.LENGTH_SHORT).show();
                        fm.beginTransaction().remove(spinnerFragment).commitAllowingStateLoss();
                        return;
                    }

                    // Reset pagination state
                    currentSeasonIndex = 0;
                    currentPage = 0;

                    // Add Season Selector Row
                    addSeasonSelectorRow();

                    // Add Episodes Row (first 10 episodes of first season)
                    loadEpisodesForCurrentSeason();

                    // Add Pagination Row
                    updatePaginationRow();

                    // Add Related Content Row
                    ArrayObjectAdapter relatedAdapter = new ArrayObjectAdapter(new RelatedPresenter(getActivity()));
                    if (response.body().getRelatedTvseries() != null) {
                        for (RelatedMovie model : response.body().getRelatedTvseries()) {
                            model.setType("tvseries");
                            relatedAdapter.add(model);
                        }
                    }
                    HeaderItem relatedHeader = new HeaderItem(4, "You may also like");
                    mAdapter.add(new ListRow(relatedHeader, relatedAdapter));

                    fm.beginTransaction().remove(spinnerFragment).commitAllowingStateLoss();
                } else {
                    fm.beginTransaction().remove(spinnerFragment).commitAllowingStateLoss();
                }
            }

            @Override
            public void onFailure(Call<MovieSingleDetails> call, Throwable t) {
                fm.beginTransaction().remove(spinnerFragment).commitAllowingStateLoss();
            }
        });

    }

    private void addToFav() {
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService api = retrofit.create(ApiService.class);
        Call<FavoriteModel> call = api.addToFavorite(AppConfig.API_KEY, userId, id);
        call.enqueue(new Callback<FavoriteModel>() {
            @Override
            public void onResponse(Call<FavoriteModel> call, Response<FavoriteModel> response) {
                if (response.code() == 200) {
                    if (response.body().getStatus().equalsIgnoreCase("success")) {
                        favStatus = true;
                        new ToastMsg(getActivity()).toastIconSuccess(response.body().getMessage());
                        setActionAdapter(favStatus);
                    } else {
                        favStatus = false;
                        new ToastMsg(getActivity()).toastIconError(getString(R.string.you_are_not_logged_in));
                    }
                }
            }

            @Override
            public void onFailure(Call<FavoriteModel> call, Throwable t) {
                new ToastMsg(getActivity()).toastIconError(getString(R.string.error_toast));
            }
        });

    }

    private void getFavStatus() {
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService api = retrofit.create(ApiService.class);
        Call<FavoriteModel> call = api.verifyFavoriteList(AppConfig.API_KEY, userId, id);
        call.enqueue(new Callback<FavoriteModel>() {
            @Override
            public void onResponse(Call<FavoriteModel> call, Response<FavoriteModel> response) {
                if (response.code() == 200) {
                    if (response.body().getStatus().equalsIgnoreCase("success")) {
                        favStatus = true;
                        setActionAdapter(favStatus);
                    } else {
                        favStatus = false;
                        setActionAdapter(false);
                    }
                }
            }

            @Override
            public void onFailure(Call<FavoriteModel> call, Throwable t) {
                new ToastMsg(getActivity()).toastIconError(getString(R.string.fetch_error));
            }
        });
    }

    private void removeFromFav() {
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ApiService api = retrofit.create(ApiService.class);
        Call<FavoriteModel> call = api.removeFromFavorite(AppConfig.API_KEY, userId, id);
        call.enqueue(new Callback<FavoriteModel>() {
            @Override
            public void onResponse(Call<FavoriteModel> call, Response<FavoriteModel> response) {
                if (response.code() == 200) {
                    if (response.body().getStatus().equalsIgnoreCase("success")) {
                        favStatus = false;
                        new ToastMsg(getActivity()).toastIconSuccess(response.body().getMessage());
                        setActionAdapter(favStatus);
                    } else {
                        favStatus = true;
                        new ToastMsg(getActivity()).toastIconError(response.body().getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<FavoriteModel> call, Throwable t) {
                new ToastMsg(getActivity()).toastIconError(getString(R.string.fetch_error));
            }
        });
    }

    private void getPhim4kData(String vtype, final String vId) {
        final SpinnerFragment spinnerFragment = new SpinnerFragment();
        final FragmentManager fm = getFragmentManager();
        fm.beginTransaction().add(R.id.details_fragment, spinnerFragment).commit();

        // Extract actual phim4k ID from full ID (remove "phim4k_" prefix)
        String actualId = vId.replace("phim4k_", "");
        Log.e(TAG, "Getting phim4k data for: " + actualId);

        Phim4kClient phim4kClient = Phim4kClient.getInstance();
        phim4kClient.getMovieDetailWithMovie(actualId, new Phim4kClient.Phim4kDetailWithMovieCallback() {
            @Override
            public void onSuccess(VideoContent videoContent, Phim4kMovie phim4kMovie) {
                if (getActivity() == null) return;
                
                Log.e(TAG, "Phim4k details received: " + videoContent.getTitle());
                
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Create a MovieSingleDetails object with phim4k data
                        MovieSingleDetails phim4kDetails = new MovieSingleDetails();
                        phim4kDetails.setTitle(videoContent.getTitle());
                        phim4kDetails.setDescription(videoContent.getDescription());
                        phim4kDetails.setPosterUrl(videoContent.getThumbnailUrl());
                        phim4kDetails.setRelease(videoContent.getRelease());
                        phim4kDetails.setType(vtype);
                        phim4kDetails.setVideosId(vId);
                        phim4kDetails.setIsPaid("0"); // Phim4k content is always free
                        
                        // Convert phim4k episodes to Video objects for playback
                        if (phim4kMovie != null && phim4kMovie.getEpisodes() != null) {
                            List<Video> videos = new ArrayList<>();
                            for (Phim4kEpisodeServer episodeServer : phim4kMovie.getEpisodes()) {
                                if (episodeServer.getItems() != null) {
                                    for (Phim4kEpisode episode : episodeServer.getItems()) {
                                        Video video = new Video();
                                        video.setLabel(episodeServer.getServerName() + " - " + episode.getName());
                                        video.setFileUrl(episode.getLink());
                                        video.setVideoFileId(episode.getSlug());
                                        video.setFileType("m3u8"); // Assuming HLS format
                                        video.setSubtitle(new ArrayList<>()); // Initialize empty subtitle list
                                        videos.add(video);
                                    }
                                }
                            }
                            phim4kDetails.setVideos(videos);
                            Log.e(TAG, "Created " + videos.size() + " video links for phim4k content");
                        }
                        
                        isPaid = "0";
                        
                        // Set action adapter
                        if (vtype.equals("movie")) {
                            setMovieActionAdapter(favStatus);
                        } else {
                            setTvSeriesActionAdapter(favStatus);
                        }
                        
                        // Bind the details to UI
                        bindMovieDetails(phim4kDetails);
                        
                        // For TV series, create episode list from phim4k data
                        if (vtype.equals("tvseries") && phim4kMovie != null && phim4kMovie.getEpisodes() != null) {
                            // Create episodes list for TV series
                            List<Episode> episodes = new ArrayList<>();
                            int episodeNumber = 1;
                            
                            for (Phim4kEpisodeServer episodeServer : phim4kMovie.getEpisodes()) {
                                if (episodeServer.getItems() != null) {
                                    for (Phim4kEpisode phim4kEpisode : episodeServer.getItems()) {
                                        Episode episode = new Episode();
                                        episode.setEpisodesId(phim4kEpisode.getSlug());
                                        episode.setEpisodesName(phim4kEpisode.getName());
                                        episode.setFileUrl(phim4kEpisode.getLink());
                                        episode.setFileType("m3u8");
                                        episode.setImageUrl(videoContent.getThumbnailUrl());
                                        episode.setIsPaid("0");
                                        episode.setSeasonName(episodeServer.getServerName());
                                        episode.setTvSeriesTitle(videoContent.getTitle());
                                        episode.setCardBackgroundUrl(videoContent.getThumbnailUrl());
                                        episode.setVideosId(vId); // Set the main phim4k series ID for watch history tracking
                                        episodes.add(episode);
                                        episodeNumber++;
                                    }
                                }
                            }
                            
                            // Create a season row for episodes
                            if (!episodes.isEmpty()) {
                                ArrayObjectAdapter rowAdapter = new ArrayObjectAdapter(new EpisodPresenter());
                                for (Episode episode : episodes) {
                                    rowAdapter.add(episode);
                                }
                                HeaderItem header = new HeaderItem(0, "Episodes");
                                mAdapter.add(new ListRow(header, rowAdapter));
                                Log.e(TAG, "Added " + episodes.size() + " episodes to phim4k TV series");
                            }
                        }
                        
                        fm.beginTransaction().remove(spinnerFragment).commitAllowingStateLoss();
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (getActivity() == null) return;
                
                Log.e(TAG, "Phim4k error: " + error);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        fm.beginTransaction().remove(spinnerFragment).commitAllowingStateLoss();
                        Toast.makeText(mContext, "Error loading phim4k content: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        
        Log.e(TAG, "onResume: VideoDetailsFragment resumed");
        
        // Add small delay to ensure UI is stable after returning from player
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (getActivity() == null || !isAdded()) {
                    Log.w(TAG, "onResume: Fragment not attached, skipping data check");
                    return;
                }
                
                checkAndReloadDataIfNeeded();
            }
        }, 100); // 100ms delay
    }
    
    private void checkAndReloadDataIfNeeded() {
        // Check if we have valid data, if not reload
        boolean needsReload = false;
        
        if (movieDetails == null) {
            Log.e(TAG, "checkAndReloadDataIfNeeded: movieDetails is null, needs reload");
            needsReload = true;
        }
        
        if (mDetailsOverviewRow == null) {
            Log.e(TAG, "checkAndReloadDataIfNeeded: mDetailsOverviewRow is null, needs reload");
            needsReload = true;
        }
        
        if (mAdapter == null) {
            Log.e(TAG, "checkAndReloadDataIfNeeded: mAdapter is null, recreating");
            setUpAdapter();
            needsReload = true;
        }
        
        // If adapter exists but has no items, also reload
        if (mAdapter != null && mAdapter.size() == 0) {
            Log.e(TAG, "checkAndReloadDataIfNeeded: mAdapter has no items, needs reload");
            needsReload = true;
        }
        
        if (needsReload && id != null && type != null) {
            Log.e(TAG, "checkAndReloadDataIfNeeded: Reloading data for id=" + id + ", type=" + type);
            
            // Clear any existing data to prevent conflicts
            if (mAdapter != null) {
                mAdapter.clear();
            }
            
            // Recreate the details overview row
            setUpDetailsOverviewRow();
        } else if (!needsReload) {
            Log.e(TAG, "checkAndReloadDataIfNeeded: Data is valid, no reload needed");
        }
    }
    
    @Override
    public void onStart() {
        super.onStart();
        
        // Ensure background is properly set when returning from player
        if (bgHelper != null && thumbUrl != null) {
            bgHelper.updateBackground(thumbUrl);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        movieDetails = null;
    }

    // ==================== TV SERIES PAGINATION METHODS ====================

    /**
     * Add season selector row with button to open season selection dialog
     */
    private void addSeasonSelectorRow() {
        if (allSeasons == null || allSeasons.isEmpty()) {
            return;
        }

        SeasonSelectorItem item = new SeasonSelectorItem();
        item.setCurrentSeasonName(allSeasons.get(currentSeasonIndex).getSeasonsName());
        item.setCurrentSeasonIndex(currentSeasonIndex);
        item.setAllSeasons(allSeasons);

        // Create and store presenter
        seasonSelectorPresenter = new SeasonSelectorPresenter(this);
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(seasonSelectorPresenter);
        adapter.add(item);

        HeaderItem header = new HeaderItem(1, "");
        seasonSelectorRow = new ListRow(header, adapter);
        mAdapter.add(seasonSelectorRow);
    }

    /**
     * Show dialog to select season
     * Called from SeasonSelectorPresenter when button is clicked
     */
    public void showSeasonDialog() {
        if (allSeasons == null || allSeasons.isEmpty()) {
            return;
        }

        // Create array of season names
        String[] seasonNames = new String[allSeasons.size()];
        for (int i = 0; i < allSeasons.size(); i++) {
            seasonNames[i] = allSeasons.get(i).getSeasonsName();
        }

        // Show AlertDialog with single choice
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Select Season");
        builder.setSingleChoiceItems(seasonNames, currentSeasonIndex,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which != currentSeasonIndex) {
                            Log.d(TAG, "Season changed from " + currentSeasonIndex + " to " + which);
                            currentSeasonIndex = which;
                            currentPage = 0; // Reset to first page

                            Log.d(TAG, "New season name: " + allSeasons.get(currentSeasonIndex).getSeasonsName());
                            
                            // Update UI
                            updateSeasonSelectorRow();
                            loadEpisodesForCurrentSeason();
                            updatePaginationRow();
                        }
                        dialog.dismiss();
                    }
                });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * Update season selector button text
     */
    private void updateSeasonSelectorRow() {
        if (allSeasons == null || allSeasons.isEmpty()) {
            Log.e(TAG, "updateSeasonSelectorRow: allSeasons is null or empty");
            return;
        }

        String newSeasonName = allSeasons.get(currentSeasonIndex).getSeasonsName();
        Log.d(TAG, "updateSeasonSelectorRow: Updating to season " + currentSeasonIndex + ": " + newSeasonName);

        // STEP 1: Remove ALL existing season selector rows to prevent duplicates
        for (int i = mAdapter.size() - 1; i >= 0; i--) {
            Object item = mAdapter.get(i);
            if (item instanceof ListRow) {
                ListRow row = (ListRow) item;
                if (row.getAdapter() instanceof ArrayObjectAdapter) {
                    ArrayObjectAdapter adapter = (ArrayObjectAdapter) row.getAdapter();
                    if (adapter.getPresenterSelector() != null && 
                        adapter.size() > 0 && 
                        adapter.get(0) instanceof SeasonSelectorItem) {
                        mAdapter.remove(item);
                        Log.d(TAG, "updateSeasonSelectorRow: Removed old season selector row at position " + i);
                    }
                }
            }
        }

        // STEP 2: Create COMPLETELY NEW row with NEW data
        SeasonSelectorItem item = new SeasonSelectorItem();
        item.setCurrentSeasonName(newSeasonName);
        item.setCurrentSeasonIndex(currentSeasonIndex);
        item.setAllSeasons(allSeasons);

        // Create NEW presenter and adapter
        seasonSelectorPresenter = new SeasonSelectorPresenter(this);
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(seasonSelectorPresenter);
        adapter.add(item);

        // Create NEW row
        HeaderItem header = new HeaderItem(1, "");
        seasonSelectorRow = new ListRow(header, adapter);
        
        // STEP 3: Add at position 1 (after DetailsOverviewRow)
        mAdapter.add(1, seasonSelectorRow);
        
        Log.d(TAG, "updateSeasonSelectorRow: Added new season selector row at position 1 with name: " + newSeasonName);
    }

    /**
     * Load episodes for current season with pagination
     */
    private void loadEpisodesForCurrentSeason() {
        if (allSeasons == null || allSeasons.isEmpty()) {
            return;
        }

        Season currentSeason = allSeasons.get(currentSeasonIndex);
        List<Episode> allEpisodes = currentSeason.getEpisodes();

        if (allEpisodes == null || allEpisodes.isEmpty()) {
            Toast.makeText(mContext, "No episodes found for this season", Toast.LENGTH_SHORT).show();
            return;
        }

        // Calculate pagination
        int startIndex = currentPage * EPISODES_PER_PAGE;
        int endIndex = Math.min(startIndex + EPISODES_PER_PAGE, allEpisodes.size());

        // Get episodes for current page
        List<Episode> pageEpisodes = allEpisodes.subList(startIndex, endIndex);

        // Remove old episodes row if exists
        if (episodesRow != null) {
            mAdapter.remove(episodesRow);
        }

        // Create adapter with full-width presenter with navigation data
        ArrayObjectAdapter episodesAdapter = new ArrayObjectAdapter(
            new FullWidthEpisodePresenter(currentSeasonIndex, startIndex, allSeasons)
        );

        for (int i = 0; i < pageEpisodes.size(); i++) {
            Episode episode = pageEpisodes.get(i);
            episode.setIsPaid(isPaid);
            episode.setSeasonName(currentSeason.getSeasonsName());
            episode.setTvSeriesTitle(movieDetails.getTitle());
            episode.setCardBackgroundUrl(movieDetails.getPosterUrl());
            episode.setVideosId(id);
            
            // Store the episode index in the episode object for navigation
            // This is the global index within the season (not just page index)
            episode.setEpisodeIndexInSeason(startIndex + i);

            episodesAdapter.add(episode);
        }

        HeaderItem header = new HeaderItem(2, "Episodes " + (startIndex + 1) + "-" + endIndex);
        episodesRow = new ListRow(header, episodesAdapter);
        
        // Always add at position 2 (after season selector at position 1)
        mAdapter.add(2, episodesRow);
    }

    /**
     * Update pagination row with current page info
     */
    private void updatePaginationRow() {
        if (allSeasons == null || allSeasons.isEmpty()) {
            return;
        }

        Season currentSeason = allSeasons.get(currentSeasonIndex);
        int totalEpisodes = currentSeason.getEpisodes() != null ? currentSeason.getEpisodes().size() : 0;
        int totalPages = (int) Math.ceil((double) totalEpisodes / EPISODES_PER_PAGE);

        // Remove old pagination row if exists
        if (paginationRow != null) {
            mAdapter.remove(paginationRow);
        }

        PaginationItem item = new PaginationItem();
        item.setCurrentPage(currentPage);
        item.setTotalPages(totalPages);
        item.setTotalEpisodes(totalEpisodes);

        ArrayObjectAdapter adapter = new ArrayObjectAdapter(new PaginationPresenter(this));
        adapter.add(item);

        HeaderItem header = new HeaderItem(3, "");
        paginationRow = new ListRow(header, adapter);
        
        // Always add at position 3 (after episodes at position 2)
        mAdapter.add(3, paginationRow);
    }

    /**
     * Handle page change event from pagination buttons
     * Called from PaginationPresenter when page button is clicked
     */
    public void onPageChanged(int newPage) {
        if (allSeasons == null || allSeasons.isEmpty()) {
            return;
        }

        Season currentSeason = allSeasons.get(currentSeasonIndex);
        int totalEpisodes = currentSeason.getEpisodes() != null ? currentSeason.getEpisodes().size() : 0;
        int totalPages = (int) Math.ceil((double) totalEpisodes / EPISODES_PER_PAGE);

        // Validate page number
        if (newPage < 0 || newPage >= totalPages) {
            return;
        }

        currentPage = newPage;
        loadEpisodesForCurrentSeason();
        updatePaginationRow();
    }
}

