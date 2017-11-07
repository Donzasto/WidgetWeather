package com.example.artem.tz_widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.widget.RemoteViews;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;


public class MyWidget extends AppWidgetProvider {

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        RequestTask rt = new RequestTask();
        rt.execute();
        String sTemp = "";
        try {
            sTemp = rt.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        for(int i : appWidgetIds){
            updateWidget(context, appWidgetManager, i, sTemp);
        }
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, String sTemp) {
        //Заносим данные в поле
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);
        remoteViews.setTextViewText(R.id.tv, sTemp);
        //Подготавливаем Intent для Broadcast
        Intent updateIntent = new Intent(context, MyWidget.class);
        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] {appWidgetId});
        //Создаем событие
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, updateIntent, 0);
        //Регистрируем событие
        remoteViews.setOnClickPendingIntent(R.id.tv, pendingIntent);
        remoteViews.setOnClickPendingIntent(R.id.btn, pendingIntent);
        //Обновляем виджет
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }

    private class RequestTask extends AsyncTask<Void, Void, String>{

        @Override
        protected String doInBackground(Void[] params)  {
            String sTemp = "";
            try {
                //Запрос
                URL url = new URL("http://xml.meteoservice.ru/export/gismeteo/point/37.xml");
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.connect();
                InputStream is = httpURLConnection.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = br.readLine()) != null) {
                    sb.append(line);

                }
                StringReader sr = new StringReader(sb.toString());

                br.close();
                is.close();
                httpURLConnection.disconnect();

                sTemp = parseXML(sr);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return sTemp;
        }

        private String parseXML(StringReader sr) {
            String sTemp = "";
            try {
                XmlPullParser xpp = prepareXpp(sr);
                while (xpp.getEventType() != XmlPullParser.END_DOCUMENT && sTemp.isEmpty()) {
                    switch (xpp.getEventType()) {
                        case XmlPullParser.START_TAG:
                            if (xpp.getName().equals("TEMPERATURE"))
                                sTemp = "max " + xpp.getAttributeValue(0) + " min " + xpp.getAttributeValue(1);
                            break;
                        default:
                            break;
                    }
                    xpp.next();
                }
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return sTemp;
        }

        private XmlPullParser prepareXpp (StringReader params)throws XmlPullParserException {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(params);
            return xpp;
        }
    }
}
