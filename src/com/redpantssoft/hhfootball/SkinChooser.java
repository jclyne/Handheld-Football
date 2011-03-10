package com.redpantssoft.hhfootball;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.TextView;

public class SkinChooser extends DialogPreference implements OnItemSelectedListener
{
	private Context mContext;
	private int mSelectedIndex=0;
	
	public SkinChooser(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext=context;
	}

	public class SkinAdapter extends BaseAdapter 
    {
        private Context mContext;
        private int mItemBackground;
        
        TypedArray mSkins;
        private String[] mDesc;
 
        public SkinAdapter(Context context) 
        {
        	mContext=context;
        	TypedArray a = mContext.obtainStyledAttributes(R.styleable.preference_adapter);
            mItemBackground = a.getResourceId(R.styleable.preference_adapter_android_galleryItemBackground, 0);
            a.recycle();          
            
            mSkins = mContext.getResources().obtainTypedArray(R.array.skins);
            mDesc = mContext.getResources().getStringArray(R.array.skin_description);
        }
 
        public int getCount() 
        {
            return mSkins.length();
        }
 
        public Object getItem(int position) 
        {
            return position;
        }            
 
        public long getItemId(int position) 
        {
            return position;
        }
 
        public View getView(int pos, View convertView, ViewGroup parent) 
        {
            ImageView imageView = new ImageView(mContext);
            imageView.setImageDrawable(mSkins.getDrawable(pos));
            imageView.setTag(mDesc[pos]);
            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            imageView.setLayoutParams(new Gallery.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 
            													ViewGroup.LayoutParams.WRAP_CONTENT));
            imageView.setBackgroundResource(mItemBackground);
            return imageView;
        }
    }   

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		mSelectedIndex=a.getInt(index, mSelectedIndex);
		return super.onGetDefaultValue(a, index);
	}
	
	@Override
	protected void onBindDialogView(View view) 
	{
		Gallery gallery = (Gallery)view.findViewById(R.id.skin_gallery);
		gallery.setAdapter(new SkinAdapter(mContext));  
        gallery.setOnItemSelectedListener(this); 
        
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        mSelectedIndex=settings.getInt("skin", mSelectedIndex);
        gallery.setSelection(mSelectedIndex);
		super.onBindDialogView(view);
	}
	
	public void onItemSelected(AdapterView<?> parent, View view, int pos,long id)
	{
		mSelectedIndex=pos;
		
		try
		{
			TextView textView = (TextView)getDialog().findViewById(R.id.skin_description);
			textView.setText((String)view.getTag());
		}
		catch (NullPointerException e)
		{
			
		}
	}

	public void onNothingSelected(AdapterView<?> parent)
	{
		
	}
	
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (positiveResult)
		{
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
			SharedPreferences.Editor editor = settings.edit();
			editor.putInt(getKey(), mSelectedIndex);
			editor.commit();
		}
		super.onDialogClosed(positiveResult);
	}
}
