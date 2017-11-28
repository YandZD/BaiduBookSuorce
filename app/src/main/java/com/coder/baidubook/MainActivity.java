package com.coder.baidubook;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.lzy.okgo.OkGo;
import com.lzy.okgo.convert.StringConvert;
import com.lzy.okgo.model.Response;
import com.lzy.okrx2.adapter.ObservableResponse;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Comparator;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.HttpUrl;


/**
 * Created by YandZD on 2017/11/27.
 */

public class MainActivity extends AppCompatActivity {

    private EditText etSearch;
    private RecyclerView mChaptersList;
    private String book;
    private String gid, sign, ts;
    private ChaptersAdapter mAdapter;

    ArrayList<Disposable> mDisposables = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etSearch = findViewById(R.id.etSeach);
        mChaptersList = findViewById(R.id.chaptersList);

        mAdapter = new ChaptersAdapter(new ChaptersAdapter.ClickCallBack() {
            @Override
            public void onClickCallBack(ChaptersBean bean) {
                showArticles(bean.getData_cid(), bean.getData_href());
            }
        });
        mChaptersList.setLayoutManager(new LinearLayoutManager(this));
        mChaptersList.setAdapter(mAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (Disposable d : mDisposables) {
            d.dispose();
        }
    }

    public void onSearch(View view) {
        book = etSearch.getText().toString().trim();
        if (TextUtils.isEmpty(book)) {
            return;
        }

        try {
            book = URLEncoder.encode(book, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        startSearchBook();
    }

    private void startSearchBook() {
        JsoupUtil.getUrlStream("https://m.baidu.com/s?word=" + book)
                .flatMap(new Function<Document, ObservableSource<Document>>() {
                    @Override
                    public ObservableSource<Document> apply(Document document) throws Exception {
                        //搜索gid 是否存在，如果不存则不找不到书
                        String bookUrl = null;
                        Elements elements = document.select("div.c-container a");
                        for (Element item : elements) {
                            if (item.hasAttr("data-url")) {
                                //"http://m.baidu.com/tcx?appui=alaxs&page=detail&gid=&sign=&ts=&sourceurl=http://www.book9.net/0_10/";
                                if (item.attr("data-url").contains("gid") && item.attr("data-url").contains("sourceurl")) {
                                    //匹配到
                                    gid = HttpUrl.parse(item.attr("data-url")).queryParameter("gid");
                                    sign = HttpUrl.parse(item.attr("data-url")).queryParameter("sign");
                                    ts = HttpUrl.parse(item.attr("data-url")).queryParameter("ts");
                                    bookUrl = item.attr("data-url");
                                    break;
                                }
                            }
                        }

                        if (bookUrl == null) {
                            throw new IllegalStateException("找不到gid,搜不到书");

                        } else {
                            return pageSize(bookUrl);
                        }
                    }
                })
                .flatMap(new Function<Document, ObservableSource<String>>() {
                    @Override
                    public ObservableSource<String> apply(Document document) throws Exception {
                        //获取页面的大小
                        String[] ss = document.toString().split("pageNumber:");
                        String size = ss[1].split(",")[0].replaceAll("\"", "")
                                .replaceAll(" ", "").replaceAll("\\\\", "");
                        int pageSize = Integer.parseInt(size);
                        int multiple = pageSize / 5 + 1;
                        //返回章节数据
                        return getChaptersData(multiple);
                    }
                })
                .flatMap(new Function<String, ObservableSource<Document>>() {
                    @Override
                    public ObservableSource<Document> apply(String html) throws Exception {
                        return JsoupUtil.getHtmlStream(html);
                    }
                })
                .flatMap(new Function<Document, ObservableSource<ArrayList<ChaptersBean>>>() {
                    @RequiresApi(api = Build.VERSION_CODES.N)
                    @Override
                    public ObservableSource<ArrayList<ChaptersBean>> apply(Document document) throws Exception {
                        Elements elements = document.select("li");
                        ArrayList<ChaptersBean> beans = new ArrayList<>();
                        ChaptersBean bean;
                        for (Element item : elements) {
                            bean = new ChaptersBean();
                            bean.setIndex(Integer.parseInt(item.attr("data-index")));
                            bean.setData_cid(item.attr("data-cid"));
                            bean.setData_href(item.attr("data-href"));
                            bean.setTitle(item.attr("data-title"));
                            beans.add(bean);
                        }
                        beans.sort(comparator);
                        return Observable.just(beans);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ArrayList<ChaptersBean>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        mDisposables.add(d);
                    }

                    @Override
                    public void onNext(final ArrayList<ChaptersBean> s) {
                        mAdapter.setData(s);
                    }

                    @Override
                    public void onError(Throwable e) {
                        System.out.println(e);
                        Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }


    public Observable<String> getChaptersData(int size) {
        ArrayList<io.reactivex.Observable<String>> observables = new ArrayList();

        for (int i = 1; i <= size; i++) {
            String url = "https://m.baidu.com/tcx?appui=alaxs&page=api/chapterList&gid=" + gid + "&pageNum=" + i + "&chapter_order=asc&site=&saveContent=";
            observables.add(OkGo.<String>get(url)
                    .headers("Referer", "https://m.baidu.com/s?word=" + book)
                    .converter(new StringConvert())
                    .adapt(new ObservableResponse<String>())
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .map(new Function<Response<String>, String>() {
                        @Override
                        public String apply(Response<String> stringResponse) throws Exception {
                            return stringResponse.body();
                        }
                    }));
        }

        return Observable.zipIterable(observables, new Function<Object, String>() {
            @Override
            public String apply(Object o) throws Exception {
                StringBuilder sb = new StringBuilder();
                Object[] responses = (Object[]) o;
                for (Object item : responses) {
                    sb.append(item.toString());
                }
                return sb.toString();
            }
        }, false, 5);
    }

    public Observable<Document> pageSize(String url) {
        return JsoupUtil.getUrlStream(url)
                .observeOn(Schedulers.io());
    }


    public void showArticles(String cid, String url) {
//        https://m.baidu.com/tcx?appui=alaxs&page=detail&gid=733620330&cid=733620330|11742220071798270102&sign=6e0c8bcbd8d90e836ae2c9eb88bf34b6&ts=1511855128&url=http://www.book9.net/0_10/2795150.html
        String baiduUrl = String.format("https://m.baidu.com/tcx?appui=alaxs&page=detail&gid=%s&cid=%s&sign=%s&ts=%s&url=%s", gid, cid, sign, ts, url);

        Intent intent = new Intent(this, ArticlesActivity.class);
        intent.putExtra("url", baiduUrl);
        startActivity(intent);
    }


    private Comparator comparator = new Comparator<ChaptersBean>() {
        @Override
        public int compare(ChaptersBean o1, ChaptersBean o2) {
            if (o1.getIndex() < o2.getIndex())
                return 1;
            else return -1;
        }
    };
}
