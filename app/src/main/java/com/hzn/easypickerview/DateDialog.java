package com.niceloo.teacher.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.niceloo.teacher.R;
import com.niceloo.teacher.utils.ScreenUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * 类描述：日期选择 （如有错误，自行修改）<br/>
 * Created by hyx on 2017/6/25.
 */

public class DateDialog extends Dialog {

    private EasyPickerView epv_year, epv_month, epv_day;

    //    private String[] month31 = {"1", "3", "5", "7", "8", "10", "12"};
    private String[] month30 = {"02", "04", "06", "09", "11"};
    private ArrayList<String> yearList, monthList, dayList, day31, day30, day29, day28;

    private int yearHolder, monthHolder, dayHolder;

    private TextView dateTextView;

    /**
     *
     * @param context context
     * @param dateTextView 选择日期的TextView
     */
    public DateDialog(@NonNull Context context, TextView dateTextView) {
        this(context, R.style.bottomDialog);
        this.dateTextView = dateTextView;
        initData();
    }

    public DateDialog(@NonNull Context context, @StyleRes int themeResId) {
        super(context, themeResId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_date);
        epv_year = (EasyPickerView) findViewById(R.id.epv_year);
        epv_month = (EasyPickerView) findViewById(R.id.epv_month);
        epv_day = (EasyPickerView) findViewById(R.id.epv_day);
        TextView tv_done = (TextView) findViewById(R.id.tv_done);
        TextView tv_clear = (TextView) findViewById(R.id.tv_clear);
        tv_done.setOnClickListener(v -> dismiss());
        tv_clear.setOnClickListener(v -> {dateTextView.setText("        ");});

        Window window = getWindow();
        WindowManager.LayoutParams wl = window.getAttributes();
        wl.alpha = 1.0f;// 整个窗口的半透明值，1.0表示不透明，0.0表示全透明。
        wl.gravity = Gravity.BOTTOM;//设置dialog放在布局哪个位置
        wl.width = (int) ScreenUtils.getScreenWidth(getContext());//获取屏幕宽度
        ;//设置dialog宽度，默认宽度没充满屏幕
        window.setAttributes(wl);

        initPicker();
        setCurDate();

        setCancelable(true);
        setCanceledOnTouchOutside(true);
    }

    private int curYi = -1;
    private int curMi = -1;
    private int curDi = -1;

    private void setCurDate() {
        Date date = new Date();
        SimpleDateFormat formatY = new SimpleDateFormat("yyyy");
        SimpleDateFormat formatM = new SimpleDateFormat("MM");
        SimpleDateFormat formatD = new SimpleDateFormat("dd");
        for (int i = 0; i < yearList.size(); i++) {
            if (yearList.get(i).equals(formatY.format(date))) {
                curYi = i;
                break;
            }
        }
        for (int i = 0; i < monthList.size(); i++) {
            if (monthList.get(i).equals(formatM.format(date))) {
                curMi = i;
            }
        }
        for (int i = 0; i < dayList.size(); i++) {
            if (dayList.get(i).equals(formatD.format(date))){
                curDi = i;
            }
        }
        if (curYi != -1)
            epv_year.moveTo(curYi);
    }

    private void initData() {
        //年
        Date date = new Date();
        SimpleDateFormat formatY = new SimpleDateFormat("yyyy");
        yearList = new ArrayList<>();
        for (int i = 0; i < (Integer.parseInt(formatY.format(date)) - 2005 + 1); i++) {
            yearList.add(2005 + i + "");
        }
        //月
        monthList = new ArrayList<>();
        for (int i = 1; i < 13; i++) {
            if (i < 10) {
                monthList.add("0" + i);
            } else
                monthList.add(i + "");
        }
        //日
        day31 = new ArrayList<>();
        day30 = new ArrayList<>();
        day29 = new ArrayList<>();
        day28 = new ArrayList<>();
        for (int i = 1; i < 29; i++) {
            if (i < 10)
                day28.add("0" + i);
            else
                day28.add(i + "");
        }
        day29.addAll(day28);
        day29.add("29");
        day30.addAll(day29);
        day30.add("30");
        day31.addAll(day30);
        day31.add("31");
        dayList = day31;
    }

    private void initPicker() {

        epv_year.setDataList(yearList);
        epv_year.setOnScrollChangedListener(new EasyPickerView.OnScrollChangedListener() {
            @Override
            public void onScrollChanged(int curIndex) {
                dateTextView.setText(yearList.get(curIndex) + "-" + monthList.get(monthHolder) + "-" + dayList.get(dayHolder));
            }

            @Override
            public void onScrollFinished(int curIndex) {
                yearHolder = curIndex;
                dateTextView.setText(yearList.get(curIndex) + "-" + monthList.get(monthHolder) + "-" + dayList.get(dayHolder));

                if (curMi != -1){
                    epv_month.moveTo(curMi);
                    curMi = -1;
                }else {
                    if (monthList.get(monthHolder).equals("02")){
                        if (Integer.parseInt(yearList.get(yearHolder)) % 4 != 0) {
                            dayList = day28;
                        } else {
                            dayList = day29;
                        }
                        epv_day.setDataList(dayList);
                    }
                }
            }
        });

        epv_month.setDataList(monthList);
        epv_month.setOnScrollChangedListener(new EasyPickerView.OnScrollChangedListener() {
            @Override
            public void onScrollChanged(int curIndex) {
                dateTextView.setText(yearList.get(yearHolder) + "-" + monthList.get(curIndex) + "-" + "01");
            }

            @Override
            public void onScrollFinished(int curIndex) {
                monthHolder = curIndex;
                dayHolder = 0;
                dateTextView.setText(yearList.get(yearHolder) + "-" + monthList.get(curIndex) + "-" + "01");
                if ("02".equals(monthList.get(curIndex))) {
                    if (Integer.parseInt(yearList.get(yearHolder)) % 4 != 0) {
                        dayList = day28;
                    } else {
                        dayList = day29;
                    }
                } else {
                    for (String s : month30) {
                        if (s.equals(monthList.get(curIndex))) {
                            epv_day.setDataList(day30);
                            dayList = day30;
                            if ( curDi!= -1){
                                epv_day.moveTo(curDi);
                                curDi = -1;
                            }
                            return;
                        }
                    }
                    dayList = day31;
                }

                epv_day.setDataList(dayList);
                if ( curDi!= -1){
                    epv_day.moveTo(curDi);
                    curDi = -1;
                }
            }
        });

//        epv_day.setDataList(dayList);
        epv_day.setOnScrollChangedListener(new EasyPickerView.OnScrollChangedListener() {
            @Override
            public void onScrollChanged(int curIndex) {
                dateTextView.setText(yearList.get(yearHolder) + "-" + monthList.get(monthHolder) + "-" + dayList.get(curIndex));
            }

            @Override
            public void onScrollFinished(int curIndex) {
                dayHolder = curIndex;
                dateTextView.setText(yearList.get(yearHolder) + "-" + monthList.get(monthHolder) + "-" + dayList.get(curIndex));
            }
        });
    }
}
