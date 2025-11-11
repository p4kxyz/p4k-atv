package com.files.codes.view.fragments.testFolder;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.FocusHighlight;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.PresenterSelector;
import androidx.leanback.widget.RowHeaderPresenter;
import com.files.codes.view.IconHeaderItem;
import com.files.codes.view.presenter.IconHeaderItemPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.PageRow;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.VerticalGridPresenter;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.files.codes.R;
import com.files.codes.utils.DataProvider;
import com.files.codes.view.SearchActivity;
import com.files.codes.view.fragments.CountryFragment;
import com.files.codes.view.fragments.CustomRowsFragment;
import com.files.codes.view.fragments.FavouriteFragment;
import com.files.codes.view.fragments.GenreFragment;
import com.files.codes.view.fragments.HomeFragment;
import com.files.codes.view.fragments.MoviesFragment;
import com.files.codes.view.fragments.MoviesWithFilterFragment;
import com.files.codes.view.fragments.TvSeriesFragment;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import io.reactivex.disposables.CompositeDisposable;

public class HomeNewFragment extends BrowseSupportFragment {
    private static final long HEADER_ID_HOME = 1;
    private static final String HEADER_NAME_HOME = "Trang chủ";
    private static final long HEADER_ID_MOVIE = 2;
    private static final String HEADER_NAME_MOVIE = "Phim lẻ";
    private static final long HEADER_ID_TV_SERIES = 3;
    private static final String HEADER_NAME_TV_SERIES = "Phim bộ";
    private static final long HEADER_ID_GENRE = 4;
    private static final String HEADER_NAME_GENRE = "Thể loại";
    private static final long HEADER_ID_COUNTRY = 5;
    private static final String HEADER_NAME_COUNTRY = "Quốc gia";
    private static final long HEADER_ID_FAVOURITE = 6;
    private static final String HEADER_NAME_FAVOURITE = "Yêu thích";
    private static final long HEADER_ID_ACCOUNT = 7;
    private static final String HEADER_NAME_ACCOUNT = "Tài khoản";

    private ArrayObjectAdapter mRowsAdapter;
    private BackgroundManager backgroundManager;
    private CompositeDisposable disposable;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //load home content data and save it to database
        disposable = new CompositeDisposable();
        DataProvider provider = new DataProvider(getActivity(), disposable);

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                provider.getAndSaveHomeContentDataFromServer(getActivity());
                provider.getMoviesFromServer(getActivity());
                provider.getTvSeriesDataFromServer(getActivity());
                provider.getLiveTvDataFromServer(getActivity());
            }
        });
        setupUi();
        loadData();
        backgroundManager = BackgroundManager.getInstance(getActivity());
        backgroundManager.attach(getActivity().getWindow());
        getMainFragmentRegistry().registerFragment(PageRow.class, new PageRowFragmentFactory(backgroundManager));
        setOnSearchClickedListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SearchActivity.class);
                startActivity(intent);
            }
        });
    }

    private void loadData() {
        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setAdapter(mRowsAdapter);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                createRows();
                startEntranceTransition();
            }
        }, 2000);
    }

    private void createRows() {
        IconHeaderItem headerItem1 = new IconHeaderItem(HEADER_ID_HOME, HEADER_NAME_HOME, R.drawable.outline_home_24);
        PageRow pageRow1 = new PageRow(headerItem1);
        mRowsAdapter.add(pageRow1);

        IconHeaderItem headerItem2 = new IconHeaderItem(HEADER_ID_MOVIE, HEADER_NAME_MOVIE, R.drawable.outline_movie_24);
        PageRow pageRow2 = new PageRow(headerItem2);
        mRowsAdapter.add(pageRow2);

        IconHeaderItem headerItem3 = new IconHeaderItem(HEADER_ID_TV_SERIES, HEADER_NAME_TV_SERIES, R.drawable.outline_local_movies_24);
        PageRow pageRow3 = new PageRow(headerItem3);
        mRowsAdapter.add(pageRow3);

        IconHeaderItem headerItem4 = new IconHeaderItem(HEADER_ID_GENRE, HEADER_NAME_GENRE, R.drawable.outline_folder_24);
        PageRow pageRow4 = new PageRow(headerItem4);
        mRowsAdapter.add(pageRow4);

        IconHeaderItem headerItem5 = new IconHeaderItem(HEADER_ID_COUNTRY, HEADER_NAME_COUNTRY, R.drawable.outline_outlined_flag_24);
        PageRow pageRow5 = new PageRow(headerItem5);
        mRowsAdapter.add(pageRow5);

        IconHeaderItem headerItem6 = new IconHeaderItem(HEADER_ID_FAVOURITE, HEADER_NAME_FAVOURITE, R.drawable.ic_favorite_border_black_24dp);
        PageRow pageRow6 = new PageRow(headerItem6);
        mRowsAdapter.add(pageRow6);

        IconHeaderItem headerItem7 = new IconHeaderItem(HEADER_ID_ACCOUNT, HEADER_NAME_ACCOUNT, R.drawable.ic_exit_to_app_black_24dp);
        PageRow pageRow7 = new PageRow(headerItem7);
        mRowsAdapter.add(pageRow7);

        // Set presenter selector to display icons for PageRow
        setHeaderPresenterSelector(new PresenterSelector() {
            @Override
            public Presenter getPresenter(Object item) {
                return new RowHeaderPresenter() {
                    @Override
                    public ViewHolder onCreateViewHolder(ViewGroup parent) {
                        View view = getActivity().getLayoutInflater().inflate(R.layout.icon_header_item, parent, false);
                        return new ViewHolder(view);
                    }

                    @Override
                    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
                        PageRow pageRow = (PageRow) item;
                        IconHeaderItem iconHeaderItem = (IconHeaderItem) pageRow.getHeaderItem();
                        
                        View rootView = viewHolder.view;
                        android.widget.ImageView iconView = rootView.findViewById(R.id.header_icon);
                        android.widget.TextView label = rootView.findViewById(R.id.header_label);
                        
                        int iconResId = iconHeaderItem.getIconResId();
                        if (iconResId != IconHeaderItem.ICON_NONE) {
                            iconView.setImageResource(iconResId);
                            iconView.setVisibility(View.VISIBLE);
                        } else {
                            iconView.setVisibility(View.GONE);
                        }
                        
                        label.setText(iconHeaderItem.getName());
                    }

                    @Override
                    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {}
                };
            }
        });

    }

    private void setupUi() {
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);
        setBrandColor(getResources().getColor(R.color.colorPrimary));
        // setTitle(getResources().getString(R.string.app_name)); // Ẩn title app
        setOnItemViewSelectedListener((itemViewHolder, item, rowViewHolder, row) -> {

        });

        prepareEntranceTransition();
    }

    private static class PageRowFragmentFactory extends FragmentFactory {
        private final BackgroundManager backgroundManager;

        public PageRowFragmentFactory(BackgroundManager backgroundManager) {
            this.backgroundManager = backgroundManager;
        }

        @Override
        public Fragment createFragment(Object rowObj) {
            Row row = (Row) rowObj;
            backgroundManager.setDrawable(null);
            if (row.getHeaderItem().getId() == HEADER_ID_HOME) {
                return new HomeFragment();
            } else if (row.getHeaderItem().getId() == HEADER_ID_MOVIE) {
                return new MoviesFragment();
            } else if (row.getHeaderItem().getId() == HEADER_ID_TV_SERIES) {
                return new TvSeriesFragment();
            } else if (row.getHeaderItem().getId() == HEADER_ID_GENRE) {
                return new GenreFragment();
            } else if (row.getHeaderItem().getId() == HEADER_ID_COUNTRY) {
                return new CountryFragment();
            } else if (row.getHeaderItem().getId() == HEADER_ID_FAVOURITE) {
                return new FavouriteFragment();
            }
            else if (row.getHeaderItem().getId() == HEADER_ID_ACCOUNT) {
                return new MyAccountNewFragment();
            }
            throw new IllegalArgumentException(String.format("Invalid row %s", rowObj));
        }
    }

    public static class SampleFragmentA extends GridFragment {
        private static final int COLUMNS = 4;
        private final int ZOOM_FACTOR = FocusHighlight.ZOOM_FACTOR_SMALL;
        private ArrayObjectAdapter mAdapter;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setupAdapter();
            loadData();
            getMainFragmentAdapter().getFragmentHost().notifyDataReady(getMainFragmentAdapter());
        }

        private void setupAdapter() {
            VerticalGridPresenter presenter = new VerticalGridPresenter(ZOOM_FACTOR);
            presenter.setNumberOfColumns(COLUMNS);
            setGridPresenter(presenter);

            CardPresenterSelector cardPresenter = new CardPresenterSelector(getActivity());
            mAdapter = new ArrayObjectAdapter(cardPresenter);
            setAdapter(mAdapter);

            setOnItemViewClickedListener(new OnItemViewClickedListener() {
                @Override
                public void onItemClicked(
                        Presenter.ViewHolder itemViewHolder,
                        Object item,
                        RowPresenter.ViewHolder rowViewHolder,
                        Row row) {
                    Card card = (Card) item;
                    Toast.makeText(getActivity(),
                            "Clicked on " + card.getTitle(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        private void loadData() {
            String json = Utils.inputStreamToString(getResources().openRawResource(
                    R.raw.grid_example));
            CardRow cardRow = new Gson().fromJson(json, CardRow.class);
            mAdapter.addAll(0, cardRow.getCards());
        }

    }


    public static class Utils {

        public int convertDpToPixel(Context ctx, int dp) {
            float density = ctx.getResources().getDisplayMetrics().density;
            return Math.round((float) dp * density);
        }

        /**
         * Will read the content from a given {@link InputStream} and return it as a {@link String}.
         *
         * @param inputStream The {@link InputStream} which should be read.
         * @return Returns <code>null</code> if the the {@link InputStream} could not be read. Else
         * returns the content of the {@link InputStream} as {@link String}.
         */
        public static String inputStreamToString(InputStream inputStream) {
            try {
                byte[] bytes = new byte[inputStream.available()];
                inputStream.read(bytes, 0, bytes.length);
                String json = new String(bytes);
                return json;
            } catch (IOException e) {
                return null;
            }
        }

        public static Uri getResourceUri(Context context, int resID) {
            return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                    context.getResources().getResourcePackageName(resID) + '/' +
                    context.getResources().getResourceTypeName(resID) + '/' +
                    context.getResources().getResourceEntryName(resID));
        }
    }

    public static class CardRow {
        // default is a list of cards
        public static final int TYPE_DEFAULT = 0;
        // section header
        public static final int TYPE_SECTION_HEADER = 1;
        // divider
        public static final int TYPE_DIVIDER = 2;

        @SerializedName("type")
        private int mType = TYPE_DEFAULT;
        // Used to determine whether the row shall use shadows when displaying its cards or not.
        @SerializedName("shadow")
        private boolean mShadow = true;
        @SerializedName("title")
        private String mTitle;
        @SerializedName("cards")
        private List<Card> mCards;

        public int getType() {
            return mType;
        }

        public String getTitle() {
            return mTitle;
        }

        public boolean useShadow() {
            return mShadow;
        }

        public List<Card> getCards() {
            return mCards;
        }

        public void setmType(int mType) {
            this.mType = mType;
        }

        public void setmShadow(boolean mShadow) {
            this.mShadow = mShadow;
        }

        public void setmTitle(String mTitle) {
            this.mTitle = mTitle;
        }

        public void setmCards(List<Card> mCards) {
            this.mCards = mCards;
        }
    }

    public static class Card {
        private String mTitle = "";
        private String mDescription = "";
        private String mExtraText = "";
        private String mImageUrl;
        private String mFooterColor = null;
        private String mSelectedColor = null;
        private String mLocalImageResource = null;
        private String mFooterResource = null;
        private Card.Type mType;
        private int mId;
        private int mWidth;
        private int mHeight;

        public String getTitle() {
            return mTitle;
        }

        public void setTitle(String title) {
            mTitle = title;
        }

        public String getLocalImageResource() {
            return mLocalImageResource;
        }

        public void setLocalImageResource(String localImageResource) {
            mLocalImageResource = localImageResource;
        }

        public String getFooterResource() {
            return mFooterResource;
        }

        public void setFooterResource(String footerResource) {
            mFooterResource = footerResource;
        }

        public void setType(Type type) {
            mType = type;
        }

        public void setId(int id) {
            mId = id;
        }

        public void setWidth(int width) {
            mWidth = width;
        }

        public void setHeight(int height) {
            mHeight = height;
        }

        public int getWidth() {
            return mWidth;
        }

        public int getHeight() {
            return mHeight;
        }

        public int getId() {
            return mId;
        }

        public Card.Type getType() {
            return mType;
        }

        public String getDescription() {
            return mDescription;
        }

        public void setDescription(String description) {
            mDescription = description;
        }


        public String getExtraText() {
            return mExtraText;
        }

        public void setExtraText(String extraText) {
            mExtraText = extraText;
        }

        public int getFooterColor() {
            if (mFooterColor == null) return -1;
            return Color.parseColor(mFooterColor);
        }

        public void setFooterColor(String footerColor) {
            mFooterColor = footerColor;
        }

        public int getSelectedColor() {
            if (mSelectedColor == null) return -1;
            return Color.parseColor(mSelectedColor);
        }

        public String getImageUrl() {
            return mImageUrl;
        }

        public void setSelectedColor(String selectedColor) {
            mSelectedColor = selectedColor;
        }

        public void setImageUrl(String imageUrl) {
            mImageUrl = imageUrl;
        }

        public URI getImageURI() {
            if (getImageUrl() == null) return null;
            try {
                return new URI(getImageUrl());
            } catch (URISyntaxException e) {
                Log.d("URI exception: ", getImageUrl());
                return null;
            }
        }

        public int getLocalImageResourceId(Context context) {
            return context.getResources().getIdentifier(getLocalImageResourceName(), "drawable",
                    context.getPackageName());
        }

        public String getLocalImageResourceName() {
            return mLocalImageResource;
        }

        public String getFooterLocalImageResourceName() {
            return mFooterResource;
        }

        public enum Type {
            MOVIE,
            TV_SERIES,
            GENRE,
            COUNTRY,
            FAVOURITE,
            SETTINGS,

        }
    }

}

