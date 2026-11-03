package com.fitlog.local;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import java.util.List;

public final class TrendView extends View {
    private final List<Double> values;
    private final Paint paint=new Paint(3);
    public TrendView(Context context,List<Double> values){super(context);this.values=values;setMinimumHeight((int)(120*getResources().getDisplayMetrics().density));}
    @Override protected void onMeasure(int w,int h){setMeasuredDimension(MeasureSpec.getSize(w),(int)(130*getResources().getDisplayMetrics().density));}
    @Override protected void onDraw(Canvas c){super.onDraw(c);if(values.size()<2)return;
        float pad=20*getResources().getDisplayMetrics().density,left=pad,right=getWidth()-pad,top=pad,bottom=getHeight()-pad;
        double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;for(double v:values){min=Math.min(min,v);max=Math.max(max,v);}if(max-min<1e-9){max+=1;min-=1;}
        paint.setColor(Ui.LINE);paint.setStrokeWidth(1);c.drawLine(left,bottom,right,bottom,paint);c.drawLine(left,(top+bottom)/2,right,(top+bottom)/2,paint);
        Path path=new Path();for(int i=0;i<values.size();i++){float x=left+(right-left)*i/(values.size()-1);float y=(float)(bottom-(values.get(i)-min)/(max-min)*(bottom-top));if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}
        paint.setColor(Ui.TEAL);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3*getResources().getDisplayMetrics().density);c.drawPath(path,paint);paint.setStyle(Paint.Style.FILL);
        for(int i=0;i<values.size();i++){float x=left+(right-left)*i/(values.size()-1);float y=(float)(bottom-(values.get(i)-min)/(max-min)*(bottom-top));c.drawCircle(x,y,3*getResources().getDisplayMetrics().density,paint);}
    }
}
