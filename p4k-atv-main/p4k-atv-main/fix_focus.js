const fs = require('fs');

function updatePresenter(filePath, modelClass, varName, getMethodName) {
    let content = fs.readFileSync(filePath, 'utf8');

    let oldString = `    @Override\r\n    public ViewHolder onCreateViewHolder(ViewGroup parent) {\r\n        TextView view = new TextView(parent.getContext());\r\n\r\n        Resources res = parent.getResources();\r\n        int width = res.getDimensionPixelSize(R.dimen.country_card_width);\r\n        int height = res.getDimensionPixelSize(R.dimen.country_card_height);\r\n\r\n        view.setLayoutParams(new ViewGroup.LayoutParams(width, height));\r\n        view.setFocusable(true);\r\n        view.setFocusableInTouchMode(true);\r\n${filePath.includes('CountryPresenter') ? '       // view.setBackgroundColor(ContextCompat.getColor(parent.getContext(), R.color.colorPrimary));' : '        // view.setBackgroundColor(ContextCompat.getColor(parent.getContext(), R.color.colorPrimary));'}\r\n        view.setBackgroundResource(getColor());\r\n        view.setTextColor(Color.WHITE);\r\n        view.setGravity(Gravity.CENTER);\r\n        return new ViewHolder(view);\r\n    }\r\n\r\n    @Override\r\n    public void onBindViewHolder(ViewHolder viewHolder, Object item) {\r\n        ${modelClass} ${varName} = (${modelClass}) item;\r\n        ((TextView) viewHolder.view).setText(${varName}.${getMethodName}());\r\n${filePath.includes('GenrePresenter') ? '\r\n    }' : '    }'}`;

    // Convert line endings robustly across OS just in case
    oldString = oldString.replace(/\r\n/g, '\n');
    content = content.replace(/\r\n/g, '\n');

    let newString = `    @Override\n    public ViewHolder onCreateViewHolder(ViewGroup parent) {\n        android.widget.FrameLayout container = new android.widget.FrameLayout(parent.getContext());\n\n        Resources res = parent.getResources();\n        int width = res.getDimensionPixelSize(R.dimen.country_card_width);\n        int height = res.getDimensionPixelSize(R.dimen.country_card_height);\n\n        container.setLayoutParams(new ViewGroup.LayoutParams(width, height));\n        container.setFocusable(true);\n        container.setFocusableInTouchMode(true);\n        container.setBackgroundResource(android.R.color.transparent);\n\n        TextView view = new TextView(parent.getContext());\n        view.setLayoutParams(new android.widget.FrameLayout.LayoutParams(\n                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));\n        view.setFocusable(false);\n        view.setFocusableInTouchMode(false);\n        view.setBackgroundResource(getColor());\n        view.setTextColor(android.graphics.Color.WHITE);\n        view.setGravity(android.view.Gravity.CENTER);\n\n        android.view.View focusOverlay = new android.view.View(parent.getContext());\n        focusOverlay.setLayoutParams(new android.widget.FrameLayout.LayoutParams(\n                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));\n        focusOverlay.setBackgroundResource(R.drawable.card_focus_border);\n        focusOverlay.setVisibility(android.view.View.INVISIBLE);\n\n        container.addView(view);\n        container.addView(focusOverlay);\n\n        container.setOnFocusChangeListener(new android.view.View.OnFocusChangeListener() {\n            @Override\n            public void onFocusChange(android.view.View v, boolean hasFocus) {\n                if (hasFocus) {\n                    focusOverlay.setVisibility(android.view.View.VISIBLE);\n                    v.animate().scaleX(1.08f).scaleY(1.08f).setDuration(150).start();\n                } else {\n                    focusOverlay.setVisibility(android.view.View.INVISIBLE);\n                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();\n                }\n            }\n        });\n\n        return new ViewHolder(container);\n    }\n\n    @Override\n    public void onBindViewHolder(ViewHolder viewHolder, Object item) {\n        ${modelClass} ${varName} = (${modelClass}) item;\n        android.widget.FrameLayout container = (android.widget.FrameLayout) viewHolder.view;\n        TextView tv = (TextView) container.getChildAt(0);\n        tv.setText(${varName}.${getMethodName}());\n    }`;

    // Normalize spacing if needed
    if (content.includes(oldString)) {
        content = content.replace(oldString, newString);
        fs.writeFileSync(filePath, content, 'utf8');
        console.log('Success for ' + filePath);
    } else {
        console.log('Failed to match string in ' + filePath);
    }
}

updatePresenter('app/src/main/java/com/files/codes/view/presenter/CountryPresenter.java', 'CountryModel', 'countryModel', 'getName');
updatePresenter('app/src/main/java/com/files/codes/view/presenter/GenrePresenter.java', 'Genre', 'genre', 'getName');
