package com.redpantssoft.hhfootball;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;

/**
 * A custom dialog preference that implements the uniform selection view
 * for HandHeld football.
 *
 * @author Jeff Clyne
 */
public class UniformChooser extends DialogPreference {
    /**
     * The current context of the owning activity
     */
    private final Context context;

    /**
     * The preference value. The home uniform selection is stored in the
     * least significant 8 bits and the visitors in the next 8 bits
     */
    private int setting = 256;

    /**
     * @param context context of the owning activity
     * @param attrs   current attribute set
     */
    public UniformChooser(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    /**
     * @return the current home selection from the combined setting value
     */
    private int homeSelection() {
        return setting & 0xff;
    }

    /**
     * @return the current visitor selection from the combined setting value
     */
    private int visitorSelection() {
        return (setting >> 8) & 0xff;
    }

    /**
     * Adapter for Gallery view. It provides the data for the uniform chooser
     * gallery views from array resources for the uniform bitmaps and a string
     * descriptions
     */
    @SuppressWarnings({"SameParameterValue"})
    class UniformAdapter extends BaseAdapter {
        /**
         * Context of the owning activity
         */
        private final Context context;

        /**
         * Resource ID of the item background for the gallery items
         */
        private final int itemBackgroundResId;

        /**
         * A collection of uniform drawables
         */
        private final TypedArray uniforms;

        /**
         * An array of uniform descriptions
         */
        private final String[] desc;

        /**
         * Transformation matrix for the uniform drawables. This is applied
         * before returning the drawable to the gallery view
         */
        private final Matrix transform;

        /**
         * @param context       context of the owning activity
         * @param drawableResId resource ID of the typed array of uniform drawables
         * @param descResId     resource id of the string array of uniform descriptions
         */
        public UniformAdapter(Context context, int drawableResId, int descResId) {
            this(context, drawableResId, descResId, new Matrix());
        }

        /**
         * @param context       context of the owning activity
         * @param drawableResId resource ID of the typed array of uniform drawables
         * @param descResId     resource id of the string array of uniform descriptions
         * @param transform     transformation matrix
         */
        public UniformAdapter(Context context, int drawableResId, int descResId, Matrix transform) {
            this.context = context;
            this.transform = transform;

            //Retrieve the preferred gallery item background
            TypedArray a = this.context.obtainStyledAttributes(R.styleable.preference_adapter);
            itemBackgroundResId = a.getResourceId(R.styleable.preference_adapter_android_galleryItemBackground, 0);
            a.recycle();

            Resources r = context.getResources();
            uniforms = r.obtainTypedArray(drawableResId);
            desc = r.getStringArray(descResId);

        }


        /**
         * @return Returns the item count
         */
        @Override
        public int getCount() {
            return uniforms.length();
        }

        /**
         * Retrieves the data item at the specified position. This is a NOOP
         * since there is no data associated with the view items, only their position,
         * which correlates to the index in the drawable array.
         *
         * @param position position of the item to retrieve
         * @return item at specified position
         */

        @Override
        public Object getItem(int position) {
            return position;
        }

        /**
         * NOOP, the data has no rows
         *
         * @param position position of the item row id to retrieve
         * @return 1
         */
        @Override
        public long getItemId(int position) {
            return 1;
        }

        /**
         * Returns a view corresponding to the data at the specified position
         *
         * @param pos         The position of the item within the adapter's data set of the item whose view we want.
         * @param convertView The old view to reuse, if possible
         * @param parent      The parent that this view will eventually be attached to
         * @return A View corresponding to the data at the specified position.
         */
        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {

            /*
            This needs to manually draw the drawable into a bitmap so that
            a transformation matrix can be applied. Otherwise, the
            ImageView.setImageDrawable method would just take care of it.
            Get the drawable, for the specified position, from the drawable array
            */

            Drawable d = uniforms.getDrawable(pos);
            // Create a new bitmap that corresponds to the dimensions of the drawable
            Bitmap bm = Bitmap.createBitmap(d.getIntrinsicWidth(), d.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            // Create a new canvas to draw into the bitmap
            Canvas canvas = new Canvas(bm);
            // Draw to the canvas using all the available space
            d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            d.draw(canvas);

            // Create new bitmap that uses the transformation matrix on the existing bitmap
            bm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), transform, true);

            // Create a new image view and set the bitmap as its image and the uniform description
            //  as its tag. The image should be centered and the view should wrap to the image size
            ImageView imageView = new ImageView(context);
            imageView.setImageBitmap(bm);
            imageView.setTag(desc[pos]);
            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            imageView.setLayoutParams(new Gallery.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            imageView.setBackgroundResource(itemBackgroundResId);

            return imageView;
        }

    }

    /**
     * Called when the UniformChooser is being inflated and the default value attribute needs to be read.
     *
     * @param a     The set of attributes.
     * @param index The index of the default value attribute
     * @return The default value of this preference type.
     */
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        setting = a.getInt(index, setting);

        return super.onGetDefaultValue(a, index);
    }

    /**
     * Binds Gallery Views in the content view to the UniformAdapter
     *
     * @param view The content view of the dialog
     */
    @Override
    protected void onBindDialogView(View view) {

        // Bind the Home UniformAdapter and setup callbacks for item selection
        Gallery homeGallery = (Gallery) view.findViewById(R.id.home_uniform_gallery);
        // We want to mirror image the views in the home gallery, so make a vertical flip
        //  transformation matrix
        Matrix matrix = new Matrix();
        matrix.preScale(-1, 1);
        homeGallery.setAdapter(new UniformAdapter(context, R.array.home_uniform, R.array.uniform_description, matrix));
        homeGallery.setOnItemSelectedListener(new OnItemSelectedListener() {
             @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                UniformChooser.this.onHomeItemSelected(parent, view, pos, id);
            }

             @Override public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Bind the Visitor UniformAdapter and setup callbacks for item selection
        Gallery visitorGallery = (Gallery) view.findViewById(R.id.visitor_uniform_gallery);
        visitorGallery.setAdapter(new UniformAdapter(context, R.array.visitor_uniform, R.array.uniform_description));
        visitorGallery.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                UniformChooser.this.onVisitorItemSelected(parent, view, pos, id);
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Set the current selection as the initial value
        setting = getPersistedInt(setting);
        homeGallery.setSelection(homeSelection());
        visitorGallery.setSelection(visitorSelection());

        super.onBindDialogView(view);
    }

    /**
     * Called when a home uniform item is selected in the gallery
     *
     * @param parent The AdapterView where the selection happened
     * @param view   The view within the AdapterView that was clicked
     * @param pos    The position of the view in the adapter
     * @param id     The row id of the item that is selected
     */
    @SuppressWarnings({"UnusedParameters"})
    void onHomeItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        setting = (setting & 0xff00) | (pos & 0xff);

        TextView textView = (TextView) getDialog().findViewById(R.id.home_uniform_description);
        if (textView != null) {
            textView.setText((String) view.getTag());
        }
    }

    /**
     * Called when a visitor uniform item is selected in the gallery
     *
     * @param parent The AdapterView where the selection happened
     * @param view   The view within the AdapterView that was clicked
     * @param pos    The position of the view in the adapter
     * @param id     The row id of the item that is selected
     */
    @SuppressWarnings({"UnusedParameters"})
    void onVisitorItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        setting = (setting & 0xff) | ((pos & 0xff) << 8);

        TextView textView = (TextView) getDialog().findViewById(R.id.visitor_uniform_description);
        if (textView != null) {
            textView.setText((String) view.getTag());
        }
    }

    /**
     * Called when the UniformChooser dialog is dismissed. This will save the
     * data into the SharedPreferences.
     *
     * @param positiveResult Whether the positive button was clicked (true),
     *                       or the negative button was clicked or the dialog was canceled (false).
     */
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            SharedPreferences.Editor editor = getSharedPreferences().edit();
            editor.putInt(getKey(), setting);
            editor.commit();
        }
        super.onDialogClosed(positiveResult);
    }
}
