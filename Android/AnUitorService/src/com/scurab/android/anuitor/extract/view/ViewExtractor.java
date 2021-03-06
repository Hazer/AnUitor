package com.scurab.android.anuitor.extract.view;

import android.graphics.Rect;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.scurab.android.anuitor.extract.Translator;
import com.scurab.android.anuitor.extract.BaseExtractor;
import com.scurab.android.anuitor.extract.DetailExtractor;
import com.scurab.android.anuitor.hierarchy.ExportField;
import com.scurab.android.anuitor.hierarchy.ExportView;
import com.scurab.android.anuitor.hierarchy.IdsHelper;
import com.scurab.android.anuitor.tools.Executor;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Objects;

/**
 * User: jbruchanov
 * Date: 12/05/2014
 * Time: 14:20
 */
public class ViewExtractor extends BaseExtractor<View> {
    private static final int[] POSITION = new int[2];
    private static final Rect RECT = new Rect();

    /**
     * Those values must be present in every View related dataset<br/>
     * ScaleXY, _Visibility, _Position, LocationScreenX, Height, Width as {@link Number}<br/>
     * _RenderViewContent as {@link java.lang.Boolean}<br/>
     * Type as {@link java.lang.String}
     *
     */
    public static final String[] MANDATORY_KEYS = {"_ScaleX", "_ScaleY", "_Visibility", "_RenderViewContent", "Position", "LocationScreenX", "LocationScreenY", "Height", "Width", "Type"};

    public ViewExtractor(Translator translator) {
        super(translator);
    }

    public HashMap<String, Object> fillValues(final View v, final HashMap<String, Object> data, final HashMap<String, Object> parentData) {
        /*
         * needs to be run sometimes in main thread, specifically when in getPadding... -> resolvePadding() is called
         * If crashed internally, new fragments were not visible (put app into background and open it again will solve the issue)
         */
        Executor.runInMainThreadBlocking(new Runnable() {
            @Override
            public void run() {
                fillValuesImpl(v, data, parentData);
            }
        });
        return data;
    }

    private HashMap<String, Object> fillValuesImpl(View v, HashMap<String, Object> data, HashMap<String, Object> parentData) {
        Translator translator = getTranslator();

        data.put("Type", String.valueOf(v.getClass().getName()));
        data.put("Extractor", getClass().getName());

        data.put("Baseline", v.getBaseline());
        data.put("Background", String.valueOf(v.getBackground()));
        data.put("Context", String.valueOf(v.getContext()));
        data.put("ContentDescription", String.valueOf(v.getContentDescription()));
        data.put("IsClickable", v.isClickable());
        data.put("IsLongClickable", v.isLongClickable());
        data.put("IsEnabled", v.isEnabled());
        data.put("IsFocusable", v.isFocusable());
        data.put("IsFocusableInTouchMode", v.isFocusableInTouchMode());
        data.put("IsDuplicateParentState", v.isDuplicateParentStateEnabled());
        data.put("IsShown", v.isShown());
        data.put("KeepScreenOn", v.getKeepScreenOn());
        data.put("Left", v.getLeft());
        data.put("Top", v.getTop());
        data.put("Right", v.getRight());
        data.put("Bottom", v.getBottom());
        data.put("Width", v.getWidth());
        data.put("Height", v.getHeight());
        data.put("PaddingLeft", v.getPaddingLeft());
        data.put("PaddingTop", v.getPaddingTop());
        data.put("PaddingRight", v.getPaddingRight());
        data.put("PaddingBottom", v.getPaddingBottom());
        data.put("_Visibility", v.getVisibility());
        data.put("Visibility", translator.visibility(v.getVisibility()));

        data.put("NextFocusDownId", IdsHelper.getNameForId(v.getNextFocusDownId()));
        data.put("NextFocusLeftId", IdsHelper.getNameForId(v.getNextFocusLeftId()));
        data.put("NextFocusRightId", IdsHelper.getNameForId(v.getNextFocusRightId()));
        data.put("NextFocusUpId", IdsHelper.getNameForId(v.getNextFocusUpId()));

        data.put("ScrollX", v.getScrollX());
        data.put("ScrollY", v.getScrollY());
        data.put("Tag", String.valueOf(v.getTag()));
        data.put("HasFocus", v.hasFocus());
        data.put("HasFocusable", v.hasFocusable());
        data.put("IsOpaque", v.isOpaque());
        data.put("IsPressed", v.isPressed());
        data.put("IsSelected", v.isSelected());
        data.put("IsSoundEffects", v.isSoundEffectsEnabled());
        data.put("WillNotDraw", v.willNotDraw());
        data.put("WillNotCacheDrawing", v.willNotCacheDrawing());

        v.getHitRect(RECT);
        data.put("HitRect", RECT.toShortString());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            data.put("Alpha", v.getAlpha());
            data.put("Rotation", v.getRotation());
            data.put("RotationX", v.getRotationX());
            data.put("RotationY", v.getRotationY());
            data.put("PivotX", v.getPivotX());
            data.put("PivotY", v.getPivotY());
            data.put("TranslationX", v.getTranslationX());
            data.put("TranslationY", v.getTranslationY());
            data.put("IsHWAccelerated", v.isHardwareAccelerated());
            data.put("LayerType", translator.layerType(v.getLayerType()));
            data.put("Matrix", v.getMatrix().toShortString());
            data.put("NextFocusForwardId", IdsHelper.getNameForId(v.getNextFocusForwardId()));
            data.put("X", v.getX());
            data.put("Y", v.getY());
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            data.put("HasOnClickListener", v.hasOnClickListeners());
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (v.getResources() != null) {
                data.put("CameraDistance", v.getCameraDistance());
            }
            data.put("IsImportantForA11Y", translator.importantForA11Y(v.getImportantForAccessibility()));
            data.put("LayerDirection", translator.layoutDirection(v.getLayoutDirection()));
            data.put("MinWidth", v.getMinimumWidth());
            data.put("MinHeight", v.getMinimumHeight());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            data.put("LabelFor", IdsHelper.getNameForId(v.getLabelFor()));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            data.put("ClipBounds", String.valueOf(v.getClipBounds()));
        }

        if (Build.VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            data.put("BackgroundTintMode", String.valueOf(v.getBackgroundTintMode()));
            data.put("ClipToOutline", v.getClipToOutline());
            data.put("Elevation", v.getElevation());
            data.put("TransitionName", v.getTransitionName());
            data.put("TranslationZ", v.getTranslationZ());
            data.put("Z", v.getZ());
            data.put("NestedScrollingParent", v.hasNestedScrollingParent());
            data.put("StateListAnimator", v.getStateListAnimator());
        }

        boolean isViewGroup = (v instanceof ViewGroup) && !DetailExtractor.isExcludedViewGroup(v.getClass().getName());
        Integer isParentVisible = parentData == null ? View.VISIBLE : (Integer) parentData.get("_Visibility");
        boolean isVisible = v.getVisibility() == View.VISIBLE && (isParentVisible == null || View.VISIBLE == isParentVisible);
        boolean hasBackground = v.getBackground() != null;
        boolean shouldRender = isVisible && ((isViewGroup && hasBackground) || !isViewGroup);
        data.put("_RenderViewContent", shouldRender);

        //TODO:remove later
        data.put("RenderViewContent", shouldRender);

        fillLayoutParams(v, data, parentData);
        fillScale(v, data, parentData);
        fillLocationValues(v, data, parentData);

        if (isExportView(v)) {
            fillAnnotatedValues(v, data);
        }

        return data;
    }

    public HashMap<String, Object> fillLayoutParams(View v, HashMap<String, Object> data, HashMap<String, Object> parentData) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();

        data.put("LayoutParams", lp != null ? lp.getClass().getName() : null);

        if (lp == null) {
            return data;
        }

        data.put("layout_width", getTranslator().layoutSize(lp.width));
        data.put("layout_height", getTranslator().layoutSize(lp.height));
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
            data.put("LayoutParams_leftMargin", mlp.leftMargin);
            data.put("LayoutParams_topMargin", mlp.topMargin);
            data.put("LayoutParams_rightMargin", mlp.rightMargin);
            data.put("LayoutParams_bottomMargin", mlp.bottomMargin);
        }

        if(lp instanceof FrameLayout.LayoutParams){
            FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams) lp;
            data.put("LayoutParams_layoutGravity", getTranslator().gravity(flp.gravity));
        }

        if(lp instanceof LinearLayout.LayoutParams){
            LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams) lp;
            data.put("LayoutParams_weight", llp.weight);
            data.put("LayoutParams_layoutGravity", getTranslator().gravity(llp.gravity));
        }

        if (lp instanceof RelativeLayout.LayoutParams) {
            RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) lp;
            int[] rules = rlp.getRules();
            for (int i = 0; i < rules.length; i++) {
                int rlData = rules[i];
                if (rlData != 0) {
                    data.put(getTranslator().relativeLayoutParamRuleName(i), getTranslator().relativeLayoutParamRuleValue(rlData));
                }
            }
        }

        if(lp instanceof ViewPager.LayoutParams){
            ViewPager.LayoutParams vlp = (ViewPager.LayoutParams) lp;
            data.put("LayoutParams_layoutGravity", getTranslator().gravity(vlp.gravity));
            data.put("LayoutParams_isDecor", vlp.isDecor);
        }
        return data;
    }

    public static HashMap<String, Object> fillLocationValues(View v, HashMap<String, Object> data, HashMap<String, Object> parentData) {
        v.getLocationOnScreen(POSITION);
        data.put("LocationScreenX", POSITION[0]);
        data.put("LocationScreenY", POSITION[1]);
        POSITION[0] = POSITION[1] = 0;

        v.getLocationInWindow(POSITION);
        data.put("LocationWindowX", POSITION[0]);
        data.put("LocationWindowY", POSITION[1]);
        return data;
    }

    /**
     * Fill fields for ScaleX, ScaleY values
     *
     * @param v
     * @param data
     * @param parentData
     * @return
     */
    public static HashMap<String, Object> fillScale(View v, HashMap<String, Object> data, HashMap<String, Object> parentData) {
        float sx = 1;
        float sy = 1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            data.put("ScaleX", v.getScaleX());
            data.put("ScaleY", v.getScaleY());

            float fx = 1f;
            float fy = 1f;
            if (parentData != null) {
                Float ofx = (Float) parentData.get("_ScaleX");
                Float ofy = (Float) parentData.get("_ScaleY");
                if (ofx != null || ofy != null) {
                    fx = ofx;
                    fy = ofy;
                }
            }
            sx = v.getScaleX() * fx;
            sy = v.getScaleY() * fy;
        }

        data.put("_ScaleX", sx);
        data.put("_ScaleY", sy);
        data.put("ScaleAbsoluteX", sx);
        data.put("ScaleAbsoluteY", sy);
        return data;
    }

    /**
     * Fill annotated values
     *
     * @param v
     * @param data
     * @return
     */
    public static HashMap<String, Object> fillAnnotatedValues(View v, HashMap<String, Object> data) {
        Class<?> clz = v.getClass();
        while(clz != View.class) {//we can stop on View.class there is nothing for us
            Field[] fields = clz.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                ExportField annotation = field.getAnnotation(ExportField.class);
                if (annotation != null) {
                    try {
                        Object o = field.get(v);
                        data.put(annotation.value(), o == null ? null : String.valueOf(o));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
            clz = clz.getSuperclass();
        }
        return data;
    }

    private static boolean isExportView(View v) {
        Class<?> clz = v.getClass();
        while (clz != View.class) {//we can stop on View.class there is nothing for us
            if (clz.getAnnotation(ExportView.class) != null) {
                return true;
            }
            clz = clz.getSuperclass();
        }
        return false;
    }
}
