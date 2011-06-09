package com.redpantssoft.hhfootball;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;

/**
 * A custom dialog preference that implements the skin selection view
 * for HandHeld football.
 *
 * @author Jeff Clyne
 */

public class SkinChooser extends DialogPreference {
    /**
     * Context of the owning activity
     */
    private final Context context;

    /**
     * Current gallery item selection
     */
    private int selectedIndex = 0;

    /**
     * @param context Context of the owning activity
     * @param attrs   Attribute set of the owning context
     */
    public SkinChooser(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    /**
     * Adapter class that provides Skins to the SkinChooser gallery view
     */
    public class SkinAdapter extends BaseAdapter {
        /**
         * Context of the owning activity
         */
        private final Context context;

        /**
         * Resource ID of the item background for the gallery items
         */
        private final int itemBackground;

        /**
         * A collection of skin drawables
         */
        private final TypedArray skins;

        /**
         * A collection of skin description strings
         */
        private final String[] desc;

        /**
         * @param context Context of the owning activity
         */
        public SkinAdapter(Context context) {
            this.context = context;

            //Retrieve the preferred gallery item background
            TypedArray a = this.context.obtainStyledAttributes(R.styleable.preference_adapter);
            itemBackground = a.getResourceId(R.styleable.preference_adapter_android_galleryItemBackground, 0);
            a.recycle();

            skins = this.context.getResources().obtainTypedArray(R.array.skins);
            desc = this.context.getResources().getStringArray(R.array.skin_description);
        }

        /**
         * @return Returns the item count
         */
        @Override
        public int getCount() {
            return skins.length();
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
            return position;
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
            Create a new image view and set the bitmap as its image and the skin description
            as its tag. The image should be centered and the view should wrap to the image size
            */
            ImageView imageView = new ImageView(context);
            imageView.setImageDrawable(skins.getDrawable(pos));
            imageView.setTag(desc[pos]);
            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            imageView.setLayoutParams(new Gallery.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            imageView.setBackgroundResource(itemBackground);
            return imageView;
        }
    }

    /**
     * Called when the SkinChooser is being inflated and the default value attribute needs to be read.
     *
     * @param a     The set of attributes.
     * @param index The index of the default value attribute
     * @return The default value of this preference type.
     */
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        selectedIndex = a.getInt(index, selectedIndex);
        return super.onGetDefaultValue(a, index);
    }

    /**
     * Binds Gallery Views in the content view to the SkinAdapter
     *
     * @param view The content view of the dialog
     */
    @Override
    protected void onBindDialogView(View view) {
        Gallery gallery = (Gallery) view.findViewById(R.id.skin_gallery);
        gallery.setAdapter(new SkinAdapter(context));
        gallery.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                SkinChooser.this.OnSkinSelected(parent, view, pos, id);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Set the initial value to the current value
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        selectedIndex = settings.getInt("skin", selectedIndex);
        gallery.setSelection(selectedIndex);
        super.onBindDialogView(view);
    }

    /**
     * Called when a Skin is selected in the gallery view
     *
     * @param parent The AdapterView where the selection happened
     * @param view   The view within the AdapterView that was clicked
     * @param pos    The position of the view in the adapter
     * @param id     The row id of the item that is selected
     */
    @SuppressWarnings({"UnusedParameters"})
    void OnSkinSelected(AdapterView<?> parent, View view, int pos, long id) {
        selectedIndex = pos;

        TextView textView = (TextView) getDialog().findViewById(R.id.skin_description);
        if (textView != null)  {
            textView.setText((String) view.getTag());
        }
    }

    /**
     * Called when the SkinChooser dialog is dismissed. This will save the
     * data into the SharedPreferences.
     *
     * @param positiveResult Whether the positive button was clicked (true),
     *                       or the negative button was clicked or the dialog was canceled (false).
     */
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt(getKey(), selectedIndex);
            editor.commit();
        }
        super.onDialogClosed(positiveResult);
    }
}
