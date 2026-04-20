const fs = require('fs');

function update(filePath) {
    let content = fs.readFileSync(filePath, 'utf8');
    
    let pattern = /@Override\s+public ViewHolder onCreateViewHolder[\s\S]*?public void onBindViewHolder\(ViewHolder viewHolder, Object item\) \{[\s\S]*?\n\s*\}/m;
    
    let match = content.match(pattern);
    if (!match) {
        console.log('Failed to match regex in ' + filePath);
        return;
    }
    
    let isCountry = filePath.includes('CountryPresenter');
    let modelClass = isCountry ? 'CountryModel' : 'Genre';
    let varName = isCountry ? 'countryModel' : 'genre';
    let getMethodName = 'getName';
    
    let newText = `@Override
    public ViewHolder onCreateViewHolder(android.view.ViewGroup parent) {
        android.widget.FrameLayout container = new android.widget.FrameLayout(parent.getContext());

        android.content.res.Resources res = parent.getResources();
        int width = res.getDimensionPixelSize(com.files.codes.R.dimen.country_card_width);
        int height = res.getDimensionPixelSize(com.files.codes.R.dimen.country_card_height);

        container.setLayoutParams(new android.view.ViewGroup.LayoutParams(width, height));
        container.setFocusable(true);
        container.setFocusableInTouchMode(true);
        container.setBackgroundResource(android.R.color.transparent);

        android.widget.TextView view = new android.widget.TextView(parent.getContext());
        view.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT));
        view.setFocusable(false);
        view.setFocusableInTouchMode(false);
        view.setBackgroundResource(getColor());
        view.setTextColor(android.graphics.Color.WHITE);
        view.setGravity(android.view.Gravity.CENTER);

        android.view.View focusOverlay = new android.view.View(parent.getContext());
        focusOverlay.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT));
        focusOverlay.setBackgroundResource(com.files.codes.R.drawable.card_focus_border);
        focusOverlay.setVisibility(android.view.View.INVISIBLE);

        container.addView(view);
        container.addView(focusOverlay);

        container.setOnFocusChangeListener(new android.view.View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(android.view.View v, boolean hasFocus) {
                if (hasFocus) {
                    focusOverlay.setVisibility(android.view.View.VISIBLE);
                    v.animate().scaleX(1.08f).scaleY(1.08f).setDuration(150).start();
                } else {
                    focusOverlay.setVisibility(android.view.View.INVISIBLE);
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
                }
            }
        });

        return new androidx.leanback.widget.Presenter.ViewHolder(container);
    }

    @Override
    public void onBindViewHolder(androidx.leanback.widget.Presenter.ViewHolder viewHolder, Object item) {
        ${modelClass} ${varName} = (${modelClass}) item;
        android.widget.FrameLayout container = (android.widget.FrameLayout) viewHolder.view;
        android.widget.TextView tv = (android.widget.TextView) container.getChildAt(0);
        tv.setText(${varName}.${getMethodName}());
    }`;

    content = content.replace(pattern, newText);
    fs.writeFileSync(filePath, content, 'utf8');
    console.log('Success for ' + filePath);
}

update('app/src/main/java/com/files/codes/view/presenter/CountryPresenter.java');
update('app/src/main/java/com/files/codes/view/presenter/GenrePresenter.java');
