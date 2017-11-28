package com.coder.baidubook;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by YandZD on 2017/7/27.
 */

public class JsoupUtil {
    public static Observable<Document> getUrlStream(String url) {
        return Observable.just(url)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .map(new Function<String, Document>() {
                    @Override
                    public Document apply(String url) throws Exception {
                        Connection connect = Jsoup.connect(url);
                        connect.timeout(20 * 1000);
                        Connection data = connect.data();
                        Document document = data.get();

                        return document;
                    }
                });
    }

    public static Observable<Document> getHtmlStream(String html) {
        return Observable.just(html)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .map(new Function<String, Document>() {
                    @Override
                    public Document apply(String html) throws Exception {
                        Document document = Jsoup.parse(html);

                        return document;
                    }
                });
    }

}
