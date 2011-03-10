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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.TextView;

public class UniformChooser extends DialogPreference 
{
	private Context mContext;
	private int mSetting=256;
	
	public UniformChooser(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext=context;
	}

    private int homeSelection()
    {
    	return mSetting & 0xff;
    }
    
    private int visitorSelection()
    {
    	return (mSetting>>8) & 0xff;
    }
    
	public class UniformAdapter extends BaseAdapter 
    {
		private Context mContext;
        private int mItemBackground;
        
        private TypedArray mUniforms;
        private String[] mDesc;
        Matrix mTransform;
 
        public UniformAdapter(Context context,int drawableResId, int descResId,Matrix transform) 
        {
        	mContext=context;
            TypedArray a = mContext.obtainStyledAttributes(R.styleable.preference_adapter);
            mItemBackground = a.getResourceId(R.styleable.preference_adapter_android_galleryItemBackground, 0);
            a.recycle();    
            
            Resources r = mContext.getResources();
            mUniforms = r.obtainTypedArray(drawableResId);
        	mDesc = r.getStringArray(descResId);
        	mTransform=transform;
        }
 
        public UniformAdapter(Context context,int drawableResId, int descResId) 
        {
        	this(context,drawableResId,descResId,new Matrix());
        }

		public int getCount() 
        {
            return mUniforms.length();
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
//            imageView.setImageDrawable(mUniforms.getDrawable(pos));
            
            Drawable d = mUniforms.getDrawable(pos);
    		Bitmap bm = Bitmap.createBitmap(d.getIntrinsicWidth(),d.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bm);
			d.setBounds(0, 0, d.getIntrinsicWidth(),d.getIntrinsicHeight());
			d.draw(canvas);
			bm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), mTransform, true);
            imageView.setImageBitmap(bm);
            
            imageView.setTag(mDesc[pos]);
            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            imageView.setLayoutParams(new Gallery.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 
            													ViewGroup.LayoutParams.WRAP_CONTENT));
            imageView.setBackgroundResource(mItemBackground);
            return imageView;
        }
      
    }   

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) 
	{
		mSetting=a.getInt(index, mSetting);
		
		return super.onGetDefaultValue(a, index);
	}
	
	@Override
	protected void onBindDialogView(View view) 
	{
		Gallery homeGallery = (Gallery)view.findViewById(R.id.home_uniform_gallery);
		Matrix matrix = new Matrix();
		matrix.preScale(-1, 1);
		homeGallery.setAdapter(new UniformAdapter(mContext,R.array.home_uniform,R.array.uniform_description,matrix));  
		homeGallery.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int pos,long id)
			{
				UniformChooser.this.onHomeItemSelected(parent,view,pos,id);
			}
			
			public void onNothingSelected(AdapterView<?> parent) {}
		}); 
        
		Gallery visitorGallery = (Gallery)view.findViewById(R.id.visitor_uniform_gallery);
		visitorGallery.setAdapter(new UniformAdapter(mContext,R.array.visitor_uniform,R.array.uniform_description));  
		visitorGallery.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int pos,long id)
			{
				UniformChooser.this.onVisitorItemSelected(parent,view,pos,id);
			}
			
			public void onNothingSelected(AdapterView<?> parent) {}
		}); 
		
        mSetting=getPersistedInt(mSetting);
        homeGallery.setSelection(homeSelection());
        visitorGallery.setSelection(visitorSelection());
        
		super.onBindDialogView(view);
	}
	

	public void onHomeItemSelected(AdapterView<?> parent, View view, int pos,long id)
	{
		mSetting = (mSetting&0xff00) | (pos&0xff);
		
		try
		{
			TextView textView = (TextView)getDialog().findViewById(R.id.home_uniform_description);
			textView.setText((String)view.getTag());
		}
		catch (NullPointerException e)
		{
		}
	}
	
	public void onVisitorItemSelected(AdapterView<?> parent, View view, int pos,long id)
	{
		mSetting = (mSetting&0xff) | ((pos&0xff) << 8);
		
		try
		{
			TextView textView = (TextView)getDialog().findViewById(R.id.visitor_uniform_description);
			textView.setText((String)view.getTag());
		}
		catch (NullPointerException e)
		{
		}
	}
	
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (positiveResult)
		{
			SharedPreferences.Editor editor = getSharedPreferences().edit();
			editor.putInt(getKey(), mSetting);
			editor.commit();
		}
		super.onDialogClosed(positiveResult);
	}
}
