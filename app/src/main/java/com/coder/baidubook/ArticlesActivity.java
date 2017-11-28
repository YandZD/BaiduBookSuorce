package com.coder.baidubook;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;
import android.widget.Toast;

import com.lzy.okgo.OkGo;
import com.lzy.okgo.convert.StringConvert;
import com.lzy.okgo.model.Response;
import com.lzy.okrx2.adapter.ObservableResponse;

import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;

import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by YandZD on 2017/11/28.
 */

public class ArticlesActivity extends AppCompatActivity {

    TextView tvTitle;
    TextView tvContent;

    ArrayList<Disposable> mDisposables = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_articles);

        tvTitle = findViewById(R.id.tvTitle);
        tvContent = findViewById(R.id.tvContent);
        tvContent.setMovementMethod(ScrollingMovementMethod.getInstance());

        String url = getIntent().getStringExtra("url");
        getArticles(url);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (Disposable d : mDisposables) {
            d.dispose();
        }
    }

    private void getArticles(String url) {
        OkGo.<String>get(url)
                .converter(new StringConvert())
                .adapt(new ObservableResponse<String>())
                .subscribeOn(Schedulers.io())
                .flatMap(new Function<Response<String>, ObservableSource<Document>>() {
                    @Override
                    public ObservableSource<Document> apply(Response<String> stringResponse) throws Exception {
                        String html = stringResponse.body();
                        String[] ss = html.split("BigPipe.onPageletArrive\\(");
                        ss = ss[1].split("\\);</script>");
                        html = new JSONObject(ss[0]).getString("html");

                        return JsoupUtil.getHtmlStream(html);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Document>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        mDisposables.add(d);
                    }

                    @Override
                    public void onNext(Document document) {
                        StringBuilder sb = new StringBuilder();
                        String title = document.select("h1").get(0).text();
                        Elements elements = document.select("div.content p");

                        for (Element item : elements) {
                            sb.append(item.text() + "\n\n");
                        }
                        tvTitle.setText(title);
                        tvContent.setText(sb.toString());
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(ArticlesActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onComplete() {

                    }
                });


    }


}
