import sys
import re

with open('old_unified.txt', 'r', encoding='utf-8') as f:
    method = f.read()

method = re.sub(r'AlertDialog\.Builder builder = new AlertDialog\.Builder\(this\);\s*builder\.setTitle\([^)]+\);',
    '''android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);

        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF1E1E2E);
        root.setPadding(dp(20), dp(20), dp(20), dp(12));

        android.widget.TextView titleView = new android.widget.TextView(this);
        titleView.setText("Cài đặt phụ đề");
        titleView.setTextColor(0xFFFFFFFF);
        titleView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18);
        titleView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        titleView.setPadding(dp(4), 0, dp(4), dp(10));
        root.addView(titleView);

        ''', method)

def add_textcolor(m):
    return m.group(0) + f'\n        {m.group(1)}.setTextColor(0xFFFFFFFF);'

method = re.sub(r'TextView\s+(\w+)\s*=\s*new TextView\(this\);', add_textcolor, method)
method = re.sub(r'Switch\s+(\w+)\s*=\s*new Switch\(this\);', add_textcolor, method)

end_pattern = r'scrollView\.addView\(layout\);\s*builder\.setView\(scrollView\);\s*builder\.setPositiveButton\([^;]+;\s*builder\.show\(\);'
old_end = re.search(end_pattern, method)
if old_end:
    new_end = '''scrollView.addView(layout);
        root.addView(scrollView, new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        android.widget.Button closeBtn = new android.widget.Button(this);
        closeBtn.setText("Đóng");
        closeBtn.setTextColor(0xFFFFFFFF);
        android.graphics.drawable.GradientDrawable btnBg = new android.graphics.drawable.GradientDrawable();
        btnBg.setColor(0xFF3A3A4C);
        btnBg.setCornerRadius(dp(8));
        closeBtn.setBackground(btnBg);
        closeBtn.setOnClickListener(v -> dialog.dismiss());
        
        android.widget.LinearLayout.LayoutParams btnParams = new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        btnParams.topMargin = dp(16);
        root.addView(closeBtn, btnParams);

        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setColor(0xFF1E1E2E);
            gd.setCornerRadius(dp(16));
            dialog.getWindow().setBackgroundDrawable(gd);
            int width = (int)(getResources().getDisplayMetrics().widthPixels * 0.90); // 90% of screen width
            dialog.getWindow().setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();'''
    method = method[:old_end.start()] + new_end + method[old_end.end():]
else:
    print('Failed to find end pattern')

method = method.replace('resetBtn.setText("Khôi phục mặc định");', 
'''resetBtn.setText("Khôi phục mặc định");
        resetBtn.setTextColor(0xFFFFFFFF);
        android.graphics.drawable.GradientDrawable resetBg = new android.graphics.drawable.GradientDrawable();
        resetBg.setColor(0xFF4A4A5C);
        resetBg.setCornerRadius(dp(8));
        resetBtn.setBackground(resetBg);''')

# For finding strings with potentially mismatched encoding, lets just replace anything setting the button color
method = re.sub(r'Button\s+(speed[0-9]+)\s*=\s*new Button\(this\);(\s+)(\1\.setText\([^;]+;)(\s+)(\1\.setTextSize\([^;]+;)', 
r'Button \1 = new Button(this);\2\3\4\5\n        \1.setTextColor(0xFFFFFFFF);', method)

btn_names = ['fontSizeMinusBtn', 'fontSizePlusBtn', 'fontTypePrevBtn', 'fontTypeNextBtn', 'positionUpBtn', 'positionDownBtn', 'textColorPrevBtn', 'textColorNextBtn', 'outlineColorPrevBtn', 'outlineColorNextBtn']
for name in btn_names:
    method = re.sub(rf'Button\s+{name}\s*=\s*new Button\(this\);\s*{name}\.setText\([^;]+;',
                        lambda m: m.group(0) + f'\n        {m.group(0).split(" ")[1]}.setTextColor(0xFFFFFFFF);' +
                                             f'\n        android.graphics.drawable.GradientDrawable {m.group(0).split(" ")[1]}Bg = new android.graphics.drawable.GradientDrawable();' +
                                             f'\n        {m.group(0).split(" ")[1]}Bg.setColor(0xFF3A3A4C); {m.group(0).split(" ")[1]}Bg.setCornerRadius(dp(8)); {m.group(0).split(" ")[1]}.setBackground({m.group(0).split(" ")[1]}Bg);', method)

with open('new_unified.txt', 'w', encoding='utf-8') as f:
    f.write(method)
print('Done!')
