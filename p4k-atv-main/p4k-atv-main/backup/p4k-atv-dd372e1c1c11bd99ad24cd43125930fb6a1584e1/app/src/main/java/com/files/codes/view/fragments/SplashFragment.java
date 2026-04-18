package com.files.codes.view.fragments;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.files.codes.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SplashFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SplashFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private ImageView imageView;
    private ImageView splashBackground;
    private View sloganLeft, sloganRight;
    private View dimOverlay;

    public SplashFragment() {
        // Required empty public constructor
    }

    public static SplashFragment newInstance(String param1, String param2) {
        SplashFragment fragment = new SplashFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_splash, container, false);
        
        imageView = view.findViewById(R.id.logoView);
        splashBackground = view.findViewById(R.id.splashBackground);
        sloganLeft = view.findViewById(R.id.sloganLeft);
        sloganRight = view.findViewById(R.id.sloganRight);
        dimOverlay = view.findViewById(R.id.dimOverlay);

        // Apply blur to background
        applyBlurToBackground();

        // start entrance animations: slide left text from left, right text from right toward center
        startSloganAnimations();
        return  view;
    }

    private void startSloganAnimations() {
        if (sloganLeft == null || sloganRight == null) {
            return;
        }

        // Start with texts translated off-screen
        float screenWidth = getResources().getDisplayMetrics().widthPixels / 2f;
        sloganLeft.setTranslationX(-screenWidth);
        sloganRight.setTranslationX(screenWidth);

        // Animate to center (900ms animation)
        long duration = 900;
        sloganLeft.animate()
                .translationX(0)
                .setDuration(duration)
                .setStartDelay(300)
                .start();

        sloganRight.animate()
                .translationX(0)
                .setDuration(duration)
                .setStartDelay(300)
                .start();

        // Ensure the dim overlay is subtle: alpha already set in layout (#B3 = 70% roughly)
        if (dimOverlay != null) {
            dimOverlay.setAlpha(1f);
        }
    }

    private void applyBlurToBackground() {
        if (splashBackground == null || getContext() == null) {
            return;
        }

        try {
            // Get the drawable and convert to bitmap
            Drawable drawable = splashBackground.getDrawable();
            if (drawable == null) {
                return;
            }

            Bitmap bitmap;
            if (drawable instanceof BitmapDrawable) {
                bitmap = ((BitmapDrawable) drawable).getBitmap();
            } else {
                return;
            }

            // Create a mutable copy
            Bitmap blurredBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            
            // Apply blur using RenderScript
            RenderScript rs = RenderScript.create(getContext());
            ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
            
            Allocation input = Allocation.createFromBitmap(rs, blurredBitmap);
            Allocation output = Allocation.createTyped(rs, input.getType());
            
            // Set blur radius (0-25, where 25 is max blur)
            // 50% blur = radius 12.5
            blurScript.setRadius(12.5f);
            blurScript.setInput(input);
            blurScript.forEach(output);
            output.copyTo(blurredBitmap);
            
            // Set the blurred bitmap
            splashBackground.setImageBitmap(blurredBitmap);
            
            // Clean up
            input.destroy();
            output.destroy();
            blurScript.destroy();
            rs.destroy();
        } catch (Exception e) {
            // Silent fail
        }
    }

   }