package com.legion.streamsettings;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.OverScroller;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(new SettingsSurface(this));
    }
}

final class SettingsSurface extends FrameLayout {
    private final float d;

    SettingsSurface(Context c) {
        super(c);
        d = getResources().getDisplayMetrics().density;
        android.graphics.Bitmap bgBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.app_bg);
        if (bgBitmap != null) {
            BitmapDrawable bd = new BitmapDrawable(getResources(), bgBitmap);
            bd.setGravity(android.view.Gravity.FILL);
            setBackground(bd);
        } else {
            setBackgroundColor(Color.rgb(8, 11, 15));
        }
        build(c);
    }

    private int dp(float n) { return Math.round(n * d); }

    private void build(Context c) {
        SettingsPanel panel = new SettingsPanel(c);
        int screenHeightDp = Math.round(getResources().getDisplayMetrics().heightPixels / d);
        int panelWidth = 300;
        int panelHeight = Math.max(240, screenHeightDp - 16);

        LayoutParams params = new LayoutParams(dp(panelWidth), dp(panelHeight), Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        params.rightMargin = dp(8);
        addView(panel, params);

        // 入场动画：从右侧滑入 + 淡入
        panel.setAlpha(0f);
        panel.setTranslationX(dp(panelWidth + 8));
        panel.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(320)
                .setInterpolator(new android.view.animation.DecelerateInterpolator(2f))
                .start();

        // 演示用唤起按钮，屏幕左侧居中，交付时移除
        View trigger = new View(c) {
            @Override protected void onDraw(android.graphics.Canvas canvas) {
                android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
                p.setColor(Color.argb(180, 56, 110, 254));
                canvas.drawRoundRect(0, 0, getWidth(), getHeight(), dp(4), dp(4), p);
                p.setColor(Color.argb(230, 234, 247, 255));
                p.setTextSize(dp(11));
                p.setTextAlign(android.graphics.Paint.Align.CENTER);
                android.graphics.Paint.FontMetrics fm = p.getFontMetrics();
                canvas.drawText("设置", getWidth() / 2f, getHeight() / 2f - (fm.ascent + fm.descent) / 2f, p);
            }
        };
        trigger.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        trigger.setOnClickListener(v -> {
            panel.setVisibility(View.VISIBLE);
            panel.setAlpha(0f);
            panel.setTranslationX(dp(panelWidth + 8));
            panel.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(320)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator(2f))
                    .start();
        });
        LayoutParams triggerParams = new LayoutParams(dp(32), dp(56), Gravity.LEFT | Gravity.CENTER_VERTICAL);
        triggerParams.leftMargin = dp(4);
        addView(trigger, triggerParams);

        // 关闭按钮回调：面板淡出后隐藏
        panel.setOnCloseListener(() -> {
            panel.animate()
                    .translationX(dp(panelWidth + 8))
                    .alpha(0f)
                    .setDuration(280)
                    .setInterpolator(new android.view.animation.AccelerateInterpolator(2f))
                    .withEndAction(() -> {
                        panel.setVisibility(View.GONE);
                        panel.setTranslationX(0f);
                    })
                    .start();
        });
    }
}

final class SettingsPanel extends FrameLayout {
    private final float d;
    private final int surface = Color.rgb(27, 32, 39);
    private final int textPrimary = Color.argb(230, 234, 247, 255);
    private final int textSecondary = Color.argb(204, 234, 247, 255);
    private final int textTertiary = Color.argb(102, 234, 247, 255);
    private LinearLayout root;
    private int bitrateIndex = 0;
    private int frameRateIndex = 0;
    private int customBitrate = 3;
    private int pendingCustomBitrate = 3;
    private Runnable onCloseListener;

    interface CloseListener { void onClose(); }

    void setOnCloseListener(Runnable l) { this.onCloseListener = l; }

    SettingsPanel(Context c) {
        super(c);
        d = getResources().getDisplayMetrics().density;
        setClipToOutline(true);
        setBackground(panelBg());
        build(c);
        addView(new PanelBorder(c), new LayoutParams(-1, -1));
    }

    private int dp(float n) { return Math.round(n * d); }

    private GradientDrawable panelBg() {
        GradientDrawable g = new GradientDrawable();
        g.setColor(surface);
        g.setCornerRadius(dp(12));
        return g;
    }

    private TextView text(String value, float size, int color, int style) {
        TextView v = new TextView(getContext());
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setIncludeFontPadding(false);
        v.setGravity(Gravity.CENTER_VERTICAL);
        if (style != 0) v.setTypeface(Typeface.DEFAULT, style);
        return v;
    }

    private void build(Context c) {
        root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setClipToPadding(false);
        addView(root, new LayoutParams(-1, -1));
        showMainPage();
    }

    private LinearLayout nav(Context c, String titleText, boolean back) {
        LinearLayout nav = new LinearLayout(c);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        nav.setPadding(dp(12), 0, dp(12), 0);

        FrameLayout left = new FrameLayout(c);
        if (back) {
            ImageView arrow = new ImageView(c);
            arrow.setImageResource(R.drawable.ic_arrow_left);
            left.addView(arrow, new FrameLayout.LayoutParams(dp(16), dp(16), Gravity.CENTER));
            left.setOnClickListener(v -> showMainPage());
        }
        nav.addView(left, new LinearLayout.LayoutParams(dp(24), -1));

        TextView title = text(titleText, 14, textPrimary, 0);
        title.setGravity(Gravity.CENTER);
        nav.addView(title, new LinearLayout.LayoutParams(0, -1, 1));

        FrameLayout right = new FrameLayout(c);
        if (!back) {
            ImageView close = new ImageView(c);
            close.setImageResource(R.drawable.ic_close_large);
            close.setAlpha(0.9f);
            right.addView(close, new FrameLayout.LayoutParams(dp(16), dp(16), Gravity.CENTER));
            right.setOnClickListener(v -> {
                if (onCloseListener != null) onCloseListener.run();
            });
        }
        nav.addView(right, new LinearLayout.LayoutParams(dp(24), -1));
        return nav;
    }

    private View divider(Context c) {
        View divider = new View(c);
        divider.setBackgroundColor(Color.argb(41, 255, 255, 255));
        return divider;
    }

    private LinearLayout contentHost(Context c, int topPadding) {
        FrameLayout body = new FrameLayout(c);
        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));

        StretchScrollView scroll = new StretchScrollView(c);
        scroll.setFillViewport(false);
        scroll.setOverScrollMode(OVER_SCROLL_NEVER);
        scroll.setVerticalScrollBarEnabled(false);
        body.addView(scroll, new LayoutParams(-1, -1));

        LinearLayout content = new LinearLayout(c);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12), dp(topPadding), dp(12), dp(48));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));

        FadeFooter footer = new FadeFooter(c);
        LayoutParams footerParams = new LayoutParams(-1, dp(32), Gravity.BOTTOM);
        body.addView(footer, footerParams);

        PanelScrollBar scrollBar = new PanelScrollBar(c, scroll);
        body.addView(scrollBar, new LayoutParams(-1, -1));
        return content;
    }

    private void showMainPage() {
        Context c = getContext();
        root.removeAllViews();
        root.addView(nav(c, "设置", false), new LinearLayout.LayoutParams(-1, dp(48)));
        root.addView(divider(c), new LinearLayout.LayoutParams(-1, Math.max(1, dp(0.5f))));
        LinearLayout content = contentHost(c, 12);
        addWithGap(content, new DisplayCard(c), 0);
        addWithGap(content, overlayCard(c, "键盘透明度", "100%", 1f, R.drawable.overlay_keyboard_bg, R.drawable.keyboard_material, true), 12);
        addWithGap(content, overlayCard(c, "模拟手柄透明度", "80%", 0.8f, R.drawable.overlay_gamepad_bg, R.drawable.gamepad_material, false), 12);
    }

    private String bitrateValue() {
        if (bitrateIndex == 1) return "清晰";
        if (bitrateIndex == 2) return "高清";
        if (bitrateIndex == 3) return "超清";
        if (bitrateIndex == 4) return customBitrate + " Mbps";
        return "自动";
    }

    private String frameRateValue() {
        if (frameRateIndex == 1) return "90帧";
        if (frameRateIndex == 2) return "90帧";
        if (frameRateIndex == 3) return "144帧";
        return "30帧";
    }

    private void showBitratePage() {
        showOptionPage("码率（清晰度）", new OptionItem[]{
                new OptionItem("自动", "根据网络状况智能调节"),
                new OptionItem("清晰", "2 Mbps ｜ 适合弱网络"),
                new OptionItem("高清", "8 Mbps ｜ 流畅清晰"),
                new OptionItem("超清", "20 Mbps ｜ 极致画质")
        }, true);
    }

    private void showFrameRatePage() {
        showOptionPage("帧率（流畅度）", new OptionItem[]{
                new OptionItem("30帧", "省流省电，适合文字办公"),
                new OptionItem("90帧", "省流省电，适合文字办公"),
                new OptionItem("90帧", "高度流畅，适合演示"),
                new OptionItem("144帧", "高度流畅，适合演示")
        }, false);
    }

    private void showOptionPage(String title, OptionItem[] items, boolean bitrate) {
        Context c = getContext();
        root.removeAllViews();
        root.addView(nav(c, title, true), new LinearLayout.LayoutParams(-1, dp(48)));
        root.addView(divider(c), new LinearLayout.LayoutParams(-1, Math.max(1, dp(0.5f))));
        LinearLayout content = contentHost(c, 0);
        for (int i = 0; i < items.length; i++) {
            final int index = i;
            boolean showDivider = i < items.length - 1 || bitrate;
            boolean selected = bitrate ? bitrateIndex == i : frameRateIndex == i;
            content.addView(optionRow(c, items[i], selected, showDivider, null, () -> {
                if (bitrate) {
                    bitrateIndex = index;
                    showBitratePage();
                } else {
                    frameRateIndex = index;
                    showFrameRatePage();
                }
            }), new LinearLayout.LayoutParams(-1, dp(68)));
        }
        if (bitrate) {
            content.addView(optionRow(c, new OptionItem("自定义", ""), bitrateIndex == 4, false, customBitrate + " Mbps", () -> {
                pendingCustomBitrate = customBitrate;
                showCustomBitratePicker();
            }), new LinearLayout.LayoutParams(-1, dp(68)));
        }
    }

    private View optionRow(Context c, OptionItem item, boolean selected, boolean divider, String customValue, Runnable action) {
        LinearLayout row = new LinearLayout(c);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(12), 0, dp(12));
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setOnClickListener(v -> action.run());
        FrameLayout radioBox = new FrameLayout(c);
        radioBox.addView(new RadioDot(c, selected), new FrameLayout.LayoutParams(dp(18), dp(18), Gravity.CENTER));
        row.addView(radioBox, new LinearLayout.LayoutParams(dp(24), dp(44)));

        LinearLayout texts = new LinearLayout(c);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams textsParams = new LinearLayout.LayoutParams(0, dp(44), 1);
        textsParams.leftMargin = dp(12);
        row.addView(texts, textsParams);

        TextView title = text(item.title, 16, textPrimary, Typeface.BOLD);
        title.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        texts.addView(title, new LinearLayout.LayoutParams(-1, dp(22)));
        if (item.subtitle.length() > 0) {
            TextView sub = text(item.subtitle, 12, textTertiary, 0);
            sub.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, dp(18));
            subParams.topMargin = dp(2);
            texts.addView(sub, subParams);
        }

        if (customValue != null) {
            LinearLayout input = new LinearLayout(c);
            input.setGravity(Gravity.CENTER_VERTICAL);
            input.setPadding(dp(8), 0, dp(4), 0);
            GradientDrawable inputBg = new GradientDrawable();
            inputBg.setColor(Color.argb(10, 255, 255, 255));
            inputBg.setStroke(Math.max(1, dp(0.5f)), Color.argb(13, 234, 247, 255));
            inputBg.setCornerRadius(dp(2));
            input.setBackground(inputBg);
            TextView value = text(customValue, 12, textTertiary, 0);
            input.addView(value, new LinearLayout.LayoutParams(0, -1, 1));
            ImageView sort = new ImageView(c);
            sort.setImageResource(R.drawable.ic_sort);
            input.addView(sort, new LinearLayout.LayoutParams(dp(16), dp(16)));
            row.addView(input, new LinearLayout.LayoutParams(dp(120), dp(32)));
        }

        return new RowWithDivider(c, row, divider);
    }

    private void showCustomBitratePicker() {
        Context c = getContext();
        FrameLayout shade = new FrameLayout(c);
        shade.setClickable(true);
        shade.setBackgroundColor(Color.argb(153, 0, 0, 0));
        shade.setAlpha(0f);

        LinearLayout dialog = new LinearLayout(c);
        dialog.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.rgb(58, 64, 71), Color.rgb(58, 64, 71)});
        bg.setCornerRadius(dp(12));
        bg.setStroke(Math.max(1, dp(0.5f)), Color.argb(77, 200, 199, 254));
        dialog.setBackground(bg);
        // clip 子视图到圆角轮廓，避免 BitratePicker 绘制内容盖住顶部圆角
        dialog.setClipToOutline(true);
        dialog.setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);

        BitratePicker picker = new BitratePicker(c);
        picker.setValue(pendingCustomBitrate);
        dialog.addView(picker, new LinearLayout.LayoutParams(-1, dp(160)));

        LinearLayout buttons = new LinearLayout(c);
        buttons.setGravity(Gravity.CENTER_VERTICAL);
        buttons.setPadding(dp(12), dp(12), dp(12), dp(12));
        buttons.setOrientation(LinearLayout.HORIZONTAL);

        Runnable dismiss = () -> {
            dialog.animate()
                    .translationY(dp(40))
                    .alpha(0f)
                    .setDuration(220)
                    .setInterpolator(new android.view.animation.AccelerateInterpolator(2f))
                    .withEndAction(() -> removeView(shade))
                    .start();
            shade.animate().alpha(0f).setDuration(220).start();
        };

        buttons.addView(dialogButton(c, "取消", Color.argb(38, 234, 247, 255), v -> dismiss.run()), buttonParams(0));
        buttons.addView(dialogButton(c, "完成", Color.rgb(56, 110, 254), v -> {
            customBitrate = picker.getValue();
            pendingCustomBitrate = customBitrate;
            bitrateIndex = 4;
            dismiss.run();
            new Handler(Looper.getMainLooper()).postDelayed(this::showBitratePage, 220);
        }), buttonParams(8));
        dialog.addView(buttons, new LinearLayout.LayoutParams(-1, dp(56)));

        FrameLayout.LayoutParams dialogParams = new FrameLayout.LayoutParams(dp(240), dp(216), Gravity.CENTER);
        shade.addView(dialog, dialogParams);
        addView(shade, new LayoutParams(-1, -1));

        // 进场：从下往上 + 淡入
        dialog.setTranslationY(dp(40));
        dialog.setAlpha(0f);
        shade.animate().alpha(1f).setDuration(240).start();
        dialog.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(new android.view.animation.DecelerateInterpolator(2f))
                .start();
    }

    private LinearLayout.LayoutParams buttonParams(int leftMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(32), 1);
        params.leftMargin = dp(leftMargin);
        return params;
    }

    private TextView dialogButton(Context c, String label, int color, View.OnClickListener listener) {
        TextView button = text(label, 14, textPrimary, Typeface.NORMAL);
        button.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(4));
        button.setBackground(bg);
        button.setOnClickListener(listener);
        return button;
    }

    private final class RowWithDivider extends FrameLayout {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final boolean divider;

        RowWithDivider(Context c, View content, boolean divider) {
            super(c);
            this.divider = divider;
            setWillNotDraw(false);
            addView(content, new LayoutParams(-1, -2));
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (!divider) return;
            paint.setColor(Color.argb(13, 234, 247, 255));
            canvas.drawRect(0, getHeight() - Math.max(1, dp(0.5f)), getWidth(), getHeight(), paint);
        }
    }

    private final class RadioDot extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final boolean selected;

        RadioDot(Context c, boolean selected) {
            super(c);
            this.selected = selected;
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float r = Math.min(getWidth(), getHeight()) / 2f - Math.max(1, dp(0.5f));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1, dp(0.75f)));
            paint.setColor(selected ? Color.rgb(69, 128, 255) : Color.argb(77, 234, 247, 255));
            canvas.drawCircle(cx, cy, r, paint);
            if (selected) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.rgb(69, 128, 255));
                canvas.drawCircle(cx, cy, r, paint);
                paint.setColor(Color.WHITE);
                canvas.drawCircle(cx, cy, dp(3), paint);
            }
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private static final class OptionItem {
        final String title;
        final String subtitle;
        OptionItem(String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;
        }
    }

    private final class StretchScrollView extends ScrollView {
        private float lastY;
        private float stretch;
        private boolean stretchingFromTop;

        StretchScrollView(Context c) {
            super(c);
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                lastY = event.getY();
                cancelStretchAnimation();
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                float y = event.getY();
                float dy = y - lastY;
                lastY = y;
                if (dy > 0 && getScrollY() <= 0) {
                    pullStretch(dy, true);
                } else if (dy < 0 && isAtBottom()) {
                    pullStretch(-dy, false);
                } else if (stretch > 0f) {
                    releaseStretch();
                }
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                releaseStretch();
            }
            return super.onTouchEvent(event);
        }

        private boolean isAtBottom() {
            if (getChildCount() == 0) return false;
            int childHeight = getChildAt(0).getHeight();
            return getScrollY() + getHeight() >= childHeight;
        }

        private void pullStretch(float distance, boolean fromTop) {
            if (getChildCount() == 0) return;
            View child = getChildAt(0);
            stretchingFromTop = fromTop;
            stretch = Math.min(dp(52), stretch + distance * 0.42f);
            float scale = 1f + stretch / Math.max(1f, getHeight()) * 0.55f;
            child.setPivotX(getWidth() / 2f);
            child.setPivotY(fromTop ? 0f : child.getHeight());
            child.setScaleY(scale);
            child.setTranslationY(fromTop ? stretch * 0.24f : -stretch * 0.24f);
        }

        private void releaseStretch() {
            if (stretch <= 0f || getChildCount() == 0) return;
            View child = getChildAt(0);
            stretch = 0f;
            child.animate()
                    .scaleY(1f)
                    .translationY(0f)
                    .setDuration(260)
                    .start();
        }

        private void cancelStretchAnimation() {
            if (getChildCount() == 0) return;
            getChildAt(0).animate().cancel();
        }
    }

    private final class PanelScrollBar extends FrameLayout {
        private static final long HIDE_DELAY_MS = 1500L;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final ScrollView scroll;
        private final View thumb;
        private final Runnable hide;

        PanelScrollBar(Context c, ScrollView scroll) {
            super(c);
            this.scroll = scroll;
            setClickable(false);

            thumb = new View(c);
            GradientDrawable thumbBg = new GradientDrawable();
            thumbBg.setColor(Color.argb(38, 234, 247, 255));
            thumbBg.setCornerRadius(dp(1));
            thumb.setBackground(thumbBg);
            thumb.setAlpha(0f);
            LayoutParams thumbParams = new LayoutParams(dp(2), dp(80), Gravity.RIGHT | Gravity.TOP);
            thumbParams.rightMargin = dp(5);
            addView(thumb, thumbParams);
            hide = () -> thumb.animate().alpha(0f).setDuration(260).start();

            scroll.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                updateThumb();
                showThenHide();
            });
            scroll.post(this::updateThumb);
            scroll.getViewTreeObserver().addOnGlobalLayoutListener(this::updateThumb);
        }

        private void showThenHide() {
            if (thumb.getVisibility() != VISIBLE) return;
            thumb.animate().cancel();
            thumb.animate().alpha(1f).setDuration(160).start();
            handler.removeCallbacks(hide);
            handler.postDelayed(hide, HIDE_DELAY_MS);
        }

        private void updateThumb() {
            if (scroll.getChildCount() == 0) {
                thumb.setVisibility(GONE);
                return;
            }

            int viewport = scroll.getHeight();
            int content = scroll.getChildAt(0).getHeight();
            int range = Math.max(0, content - viewport);
            if (viewport <= 0 || range <= 0) {
                thumb.setVisibility(GONE);
                return;
            }

            thumb.setVisibility(VISIBLE);
            float trackTop = dp(12);
            float trackBottom = dp(32);
            float trackHeight = Math.max(1f, viewport - trackTop - trackBottom);
            int thumbHeight = Math.max(dp(24), Math.min(dp(80), Math.round(trackHeight * viewport / content)));
            float travel = Math.max(0f, trackHeight - thumbHeight);
            float top = trackTop + travel * scroll.getScrollY() / range;

            LayoutParams params = (LayoutParams) thumb.getLayoutParams();
            if (params.height != thumbHeight || params.topMargin != Math.round(top)) {
                params.height = thumbHeight;
                params.topMargin = Math.round(top);
                thumb.setLayoutParams(params);
            }
        }
    }

    private void addWithGap(LinearLayout content, View child, int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(top);
        content.addView(child, p);
    }

    private View overlayCard(Context c, String name, String value, float progress, int bgRes, int materialRes, boolean keyboard) {
        FrameLayout shell = new FrameLayout(c);
        shell.setClipChildren(true);
        shell.setClipToOutline(true);
        shell.setBackground(new OverlayCardDrawable(dp(2)));

        ImageView bg = new ImageView(c);
        bg.setImageResource(bgRes);
        bg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        bg.setAlpha(0.82f);
        shell.addView(bg, new FrameLayout.LayoutParams(-1, -1));
        shell.addView(new CardScrim(c), new FrameLayout.LayoutParams(-1, -1));

        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);

        LinearLayout controls = new LinearLayout(c);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setPadding(dp(12), dp(12), dp(12), dp(12));

        LinearLayout head = new LinearLayout(c);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView label = text(name, 16, textPrimary, Typeface.BOLD);
        TextView number = text(value, 14, textSecondary, 0);
        number.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        head.addView(label, new LinearLayout.LayoutParams(0, dp(24), 1));
        head.addView(number, new LinearLayout.LayoutParams(dp(70), dp(24)));
        controls.addView(head, new LinearLayout.LayoutParams(-1, dp(24)));

        FrameLayout preview = new FrameLayout(c);
        preview.setClipChildren(true);
        ImageView previewContent = new ImageView(c);
        previewContent.setImageResource(materialRes);
        previewContent.setAdjustViewBounds(false);
        previewContent.setScaleType(ImageView.ScaleType.FIT_CENTER);
        previewContent.setAlpha(progress);

        LinearLayout.LayoutParams sliderParams = new LinearLayout.LayoutParams(-1, dp(24));
        sliderParams.topMargin = dp(24);
        controls.addView(new OpacitySlider(c, progress, opacity -> {
            int percent = Math.round(opacity * 100f);
            number.setText(percent + "%");
            previewContent.setAlpha(opacity);
        }), sliderParams);
        card.addView(controls, new LinearLayout.LayoutParams(-1, dp(96)));

        FrameLayout.LayoutParams previewContentParams = keyboard
                ? new FrameLayout.LayoutParams(dp(276), dp(160), Gravity.CENTER)
                : new FrameLayout.LayoutParams(dp(276), dp(160), Gravity.CENTER);
        preview.addView(previewContent, previewContentParams);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(-1, dp(160));
        previewParams.topMargin = dp(12);
        card.addView(preview, previewParams);
        shell.addView(card, new FrameLayout.LayoutParams(-1, dp(268)));
        shell.addView(new CardBorder(c), new FrameLayout.LayoutParams(-1, -1));
        return shell;
    }

    private final class DisplayCard extends LinearLayout {
        DisplayCard(Context c) {
            super(c);
            setOrientation(VERTICAL);
            setBackground(new SoftGradientCardDrawable(dp(2)));

            LinearLayout titleWrap = new LinearLayout(c);
            titleWrap.setGravity(Gravity.CENTER_VERTICAL);
            titleWrap.setPadding(dp(12), dp(12), dp(12), dp(12));
            titleWrap.addView(text("画面设置", 16, textPrimary, Typeface.BOLD), new LinearLayout.LayoutParams(-1, dp(24)));
            addView(titleWrap, new LinearLayout.LayoutParams(-1, -2));

            LinearLayout rows = new LinearLayout(c);
            rows.setOrientation(VERTICAL);
            rows.setPadding(dp(12), 0, dp(12), dp(12));
            addView(rows, new LinearLayout.LayoutParams(-1, -2));

            rows.addView(settingRow("码率（清晰度）", bitrateValue(), () -> showBitratePage()), rowParams(0));
            rows.addView(line(), lineParams());
            rows.addView(settingRow("帧率（流畅度）", frameRateValue(), () -> showFrameRatePage()), gapParams(dp(24)));
            rows.addView(line(), lineParams());

            TextView note = text("参数越高对网络的要求更高。当网络质量不满足所选参数时，实际效果会有所差异。", 12, textTertiary, 0);
            note.setLineSpacing(dp(2), 1f);
            note.setGravity(Gravity.LEFT);
            rows.addView(note, gapParams());
        }

        private LinearLayout.LayoutParams rowParams(int top) {
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(24));
            p.topMargin = dp(top);
            return p;
        }

        private LinearLayout.LayoutParams gapParams() {
            return gapParams(-2);
        }

        private LinearLayout.LayoutParams gapParams(int height) {
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, height);
            p.topMargin = dp(8);
            return p;
        }

        private LinearLayout.LayoutParams lineParams() {
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, Math.max(1, dp(0.5f)));
            p.topMargin = dp(8);
            return p;
        }

        private LinearLayout settingRow(String left, String right, Runnable action) {
            LinearLayout row = new LinearLayout(getContext());
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setOnClickListener(v -> action.run());
            TextView leftText = text(left, 14, textSecondary, 0);
            leftText.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            row.addView(leftText, new LinearLayout.LayoutParams(0, dp(21), 1));

            LinearLayout rightGroup = new LinearLayout(getContext());
            rightGroup.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            rightGroup.setOrientation(HORIZONTAL);

            TextView value = text(right, 14, textSecondary, 0);
            value.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            rightGroup.addView(value, new LinearLayout.LayoutParams(-2, dp(21)));

            FrameLayout arrowHit = new FrameLayout(getContext());
            ImageView arrow = new ImageView(getContext());
            arrow.setImageResource(R.drawable.ic_arrow_right);
            arrowHit.addView(arrow, new FrameLayout.LayoutParams(dp(16), dp(16), Gravity.CENTER));
            LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(dp(16), dp(21));
            arrowParams.leftMargin = dp(8);
            rightGroup.addView(arrowHit, arrowParams);

            row.addView(rightGroup, new LinearLayout.LayoutParams(dp(148), dp(21)));
            return row;
        }

        private View line() {
            View v = new View(getContext());
            v.setBackgroundColor(Color.argb(13, 234, 247, 255));
            return v;
        }
    }

    private final class FadeFooter extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        FadeFooter(Context c) { super(c); }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            paint.setShader(new LinearGradient(0, 0, 0, getHeight(), Color.argb(0, 27, 32, 39), surface, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            paint.setShader(null);
        }
    }

    private final class CardScrim extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        CardScrim(Context c) { super(c); }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            paint.setShader(new LinearGradient(0, 0, 0, getHeight(), Color.rgb(45, 50, 56), Color.argb(0, 45, 50, 56), Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            paint.setShader(null);
        }
    }

    private final class CardBorder extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        CardBorder(Context c) { super(c); }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float stroke = Math.max(1f, d * 0.5f);
            float inset = stroke / 2f;
            paint.setColor(Color.argb(26, 234, 247, 255));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
            canvas.drawRoundRect(inset, inset, getWidth() - inset, getHeight() - inset, dp(2), dp(2), paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private final class PanelBorder extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        PanelBorder(Context c) {
            super(c);
            setClickable(false);
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float stroke = Math.max(1f, d * 0.5f);
            float inset = stroke / 2f;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
            paint.setColor(Color.argb(61, 255, 255, 255));
            canvas.drawRoundRect(inset, inset, getWidth() - inset, getHeight() - inset, dp(12), dp(12), paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private final class BitratePicker extends View {
        private static final int MIN_VAL = 1;
        private static final int MAX_VAL = 20;
        private static final float ITEM_DP = 36f;

        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fadePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint hlPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final OverScroller scroller;
        private VelocityTracker vt;

        // cached fade gradients — rebuilt only when view size changes
        private LinearGradient fadeTop, fadeBot;
        private int cachedW, cachedH;

        // Single source of truth: scrollY in pixels.
        // scrollY = N * itemPx()  means value N is centered.
        // Initially value=3 → scrollY = 3 * itemPx()
        private float scrollY;
        private float lastTouchY;
        private boolean dragging = false;
        private int lastHapticIndex = -1; // track last grid index for haptic

        private final Runnable ticker = new Runnable() {
            @Override public void run() {
                if (scroller.computeScrollOffset()) {
                    scrollY = scroller.getCurrY();
                    scrollY = clampScrollY(scrollY);
                    tickHaptic();
                    invalidate();
                    postOnAnimation(this);
                } else {
                    // fling done — snap to nearest integer with short animation
                    int target = Math.max(MIN_VAL, Math.min(MAX_VAL,
                            Math.round(scrollY / itemPx())));
                    float targetPx = target * itemPx();
                    if (Math.abs(scrollY - targetPx) > 1f) {
                        // use startScroll for the remaining sub-item distance
                        scroller.startScroll(0, Math.round(scrollY), 0,
                                Math.round(targetPx - scrollY), 120);
                        postOnAnimation(this); // keep ticking through snap animation
                    } else {
                        scrollY = targetPx;
                        invalidate();
                    }
                }
            }
        };

        BitratePicker(Context c) {
            super(c);
            scroller = new OverScroller(c);
            scrollY = 3 * itemPx();
            lastHapticIndex = 3;
            setClickable(true);
        }

        int getValue() {
            return Math.max(MIN_VAL, Math.min(MAX_VAL, Math.round(scrollY / itemPx())));
        }

        void setValue(int v) {
            v = Math.max(MIN_VAL, Math.min(MAX_VAL, v));
            scrollY = v * itemPx();
            lastHapticIndex = v;
            invalidate();
        }

        private float itemPx() { return dp(ITEM_DP); }

        private float clampScrollY(float y) {
            return Math.max(MIN_VAL * itemPx(), Math.min(MAX_VAL * itemPx(), y));
        }

        private void tickHaptic() {
            int idx = Math.round(scrollY / itemPx());
            if (idx != lastHapticIndex) {
                lastHapticIndex = idx;
                performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK,
                        android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            }
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    scroller.abortAnimation();
                    removeCallbacks(ticker);
                    dragging = true;
                    lastTouchY = e.getY();
                    if (vt == null) vt = VelocityTracker.obtain();
                    else vt.clear();
                    vt.addMovement(e);
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (!dragging) return true;
                    vt.addMovement(e);
                    float dy = e.getY() - lastTouchY; // 手指下移 dy 正 → scrollY 增大 → 数值增大
                    lastTouchY = e.getY();
                    scrollY += dy;
                    // boundary resistance
                    float minPx = MIN_VAL * itemPx();
                    float maxPx = MAX_VAL * itemPx();
                    if (scrollY < minPx) scrollY = minPx + (scrollY - minPx) * 0.25f;
                    if (scrollY > maxPx) scrollY = maxPx + (scrollY - maxPx) * 0.25f;
                    tickHaptic();
                    invalidate();
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    dragging = false;
                    getParent().requestDisallowInterceptTouchEvent(false);
                    if (vt != null) {
                        vt.addMovement(e);
                        vt.computeCurrentVelocity(1000);
                        float vy = vt.getYVelocity(); // 手指下移速度为正 → scrollY 增大
                        vt.recycle();
                        vt = null;
                        // snap scrollY back into hard bounds first
                        scrollY = clampScrollY(scrollY);
                        if (Math.abs(vy) > 300) {
                            scroller.fling(0, (int) scrollY, 0, (int) vy,
                                    0, 0,
                                    (int)(MIN_VAL * itemPx()), (int)(MAX_VAL * itemPx()));
                        } else {
                            // just snap
                            int target = Math.max(MIN_VAL, Math.min(MAX_VAL,
                                    Math.round(scrollY / itemPx())));
                            scroller.startScroll(0, (int) scrollY, 0,
                                    (int)(target * itemPx() - scrollY), 180);
                        }
                        postOnAnimation(ticker);
                    }
                    return true;
            }
            return true;
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float item = itemPx();

            // selection highlight bar
            hlPaint.setColor(Color.argb(61, 0, 0, 0));
            canvas.drawRect(0, cy - item / 2f, getWidth(), cy + item / 2f, hlPaint);

            // rebuild fade gradients only when size changes
            if (getWidth() != cachedW || getHeight() != cachedH) {
                cachedW = getWidth();
                cachedH = getHeight();
                fadeTop = new LinearGradient(0, 0, 0, cy - item / 2f,
                        Color.argb(210, 58, 64, 71), Color.TRANSPARENT, Shader.TileMode.CLAMP);
                fadeBot = new LinearGradient(0, cy + item / 2f, 0, getHeight(),
                        Color.TRANSPARENT, Color.argb(210, 58, 64, 71), Shader.TileMode.CLAMP);
            }

            // draw items
            float centerVal = scrollY / item;
            int lo = (int) Math.floor(centerVal) - 3;
            int hi = (int) Math.ceil(centerVal) + 3;
            for (int v = lo; v <= hi; v++) {
                if (v < MIN_VAL || v > MAX_VAL) continue;
                float offsetPx = (v - centerVal) * item;
                float slotY = cy - offsetPx;
                float distNorm = Math.abs(slotY - cy) / (getHeight() / 2f);
                if (distNorm > 1.05f) continue;

                float alpha = Math.max(0f, 1f - distNorm * 1.2f);
                float sizeDp = 10f + 6f * (float) Math.pow(1f - distNorm, 1.5f);
                textPaint.setTextAlign(Paint.Align.CENTER);
                textPaint.setTypeface(distNorm < 0.12f ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
                textPaint.setTextSize(dp(sizeDp));
                textPaint.setColor(Color.argb((int)(alpha * 255), 234, 247, 255));
                Paint.FontMetrics fm = textPaint.getFontMetrics();
                canvas.drawText(v + " Mbps", cx, slotY - (fm.ascent + fm.descent) / 2f, textPaint);
            }

            // fade overlay — always drawn last, on top of text
            fadePaint.setShader(fadeTop);
            canvas.drawRect(0, 0, getWidth(), cy - item / 2f, fadePaint);
            fadePaint.setShader(fadeBot);
            canvas.drawRect(0, cy + item / 2f, getWidth(), getHeight(), fadePaint);
        }
    }

    private final class OpacitySlider extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float progress;
        private final OnOpacityChangeListener listener;

        OpacitySlider(Context c, float progress, OnOpacityChangeListener listener) {
            super(c);
            this.progress = clamp(progress);
            this.listener = listener;
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                getParent().requestDisallowInterceptTouchEvent(true);
                updateFromX(event.getX());
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                updateFromX(event.getX());
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                getParent().requestDisallowInterceptTouchEvent(false);
                updateFromX(event.getX());
                return true;
            }
            return true;
        }

        private void updateFromX(float x) {
            float knobW = dp(28);
            float available = Math.max(1f, getWidth() - knobW);
            progress = clamp((x - knobW / 2f) / available);
            if (listener != null) listener.onChange(progress);
            invalidate();
        }

        private float clamp(float value) {
            return Math.max(0f, Math.min(1f, value));
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cy = getHeight() / 2f;
            float trackH = dp(6);
            float trackInset = dp(1);
            float knobW = dp(28);
            float knobH = dp(18);
            float left = Math.max(0, Math.min(getWidth() - knobW, (getWidth() - knobW) * progress));
            float knobCenter = left + knobW / 2f;

            paint.setShader(null);
            paint.setColor(Color.rgb(31, 37, 45));
            float trackTop = cy - trackH / 2f;
            float trackBottom = cy + trackH / 2f;
            float trackHighlightH = dp(0.5f);
            canvas.drawRect(0, trackTop, getWidth(), trackBottom, paint);
            paint.setColor(Color.argb(77, 255, 255, 255));
            canvas.drawRect(0, trackBottom, getWidth(), trackBottom + trackHighlightH, paint);
            paint.setAlpha(255);

            float progressEnd = progress >= 0.995f ? getWidth() : knobCenter;
            float innerLeft = trackInset;
            float innerRight = Math.max(innerLeft, progressEnd - trackInset);
            float innerTop = trackTop + trackInset;
            float innerBottom = trackBottom - trackInset;
            if (innerRight > innerLeft && innerBottom > innerTop) {
                paint.setShader(new LinearGradient(innerLeft, cy, innerRight, cy,
                        Color.rgb(228, 241, 250), Color.rgb(186, 204, 216), Shader.TileMode.CLAMP));
                canvas.drawRect(innerLeft, innerTop, innerRight, innerBottom, paint);
            }
            paint.setShader(null);

            paint.setShadowLayer(dp(6), 0, dp(3), Color.argb(36, 0, 0, 0));
            paint.setShader(new LinearGradient(0, cy - knobH / 2f, 0, cy + knobH / 2f, Color.rgb(203, 204, 205), Color.rgb(166, 167, 169), Shader.TileMode.CLAMP));
            canvas.drawRoundRect(left, cy - knobH / 2f, left + knobW, cy + knobH / 2f, dp(4), dp(4), paint);
            paint.setShader(null);
            paint.clearShadowLayer();
            paint.setColor(Color.argb(61, 255, 255, 255));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1, dp(0.5f)));
            canvas.drawRoundRect(left, cy - knobH / 2f, left + knobW, cy + knobH / 2f, dp(4), dp(4), paint);
            paint.setStyle(Paint.Style.FILL);
            float gripW = Math.max(0.75f, d * 0.75f);
            float gripTop = cy - dp(4);
            float gripBottom = cy + dp(4);
            float firstGrip = left + dp(10.5f);
            float secondGrip = left + dp(17.5f);
            paint.setColor(Color.argb(72, 255, 255, 255));
            canvas.drawRoundRect(firstGrip, gripTop + d, firstGrip + gripW, gripBottom + d, gripW / 2f, gripW / 2f, paint);
            canvas.drawRoundRect(secondGrip, gripTop + d, secondGrip + gripW, gripBottom + d, gripW / 2f, gripW / 2f, paint);
            paint.setColor(Color.argb(61, 0, 0, 0));
            canvas.drawRoundRect(firstGrip, gripTop, firstGrip + gripW, gripBottom, gripW / 2f, gripW / 2f, paint);
            canvas.drawRoundRect(secondGrip, gripTop, secondGrip + gripW, gripBottom, gripW / 2f, gripW / 2f, paint);
        }
    }

    private interface OnOpacityChangeListener {
        void onChange(float value);
    }

    private final class SoftGradientCardDrawable extends GradientDrawable {
        SoftGradientCardDrawable(int radius) {
            super(Orientation.TL_BR, new int[]{Color.argb(20, 255, 255, 255), Color.argb(41, 255, 255, 255)});
            setCornerRadius(radius);
            setStroke(Math.max(1, dp(0.5f)), Color.argb(26, 234, 247, 255));
        }
    }

    private final class OverlayCardDrawable extends GradientDrawable {
        OverlayCardDrawable(int radius) {
            super(Orientation.TOP_BOTTOM, new int[]{Color.rgb(45, 50, 56), Color.rgb(38, 44, 52)});
            setCornerRadius(radius);
        }
    }
}
