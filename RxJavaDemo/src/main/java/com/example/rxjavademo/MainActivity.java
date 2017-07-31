package com.example.rxjavademo;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import java.util.List;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final String[] names = {"zhang", "jbo", "jbo-zhang", "vtg", "onepiggy"};
        Observable.from(names)
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        Log.d("jbo", s);
                    }
                });

        final int drawableRes = R.drawable.img_barman;
        final ImageView imageView = (ImageView) findViewById(R.id.iv_image);
        Observable.create(new Observable.OnSubscribe<Drawable>() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void call(Subscriber<? super Drawable> subscriber) {
                Drawable drawable = getTheme().getDrawable(drawableRes);
                subscriber.onNext(drawable);
                subscriber.onCompleted();
            }
            //默认情况下，subscribe在哪个线程调用，就在哪个线程生产和消费事件
        }).subscribe(new Observer<Drawable>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Drawable drawable) {
                imageView.setImageDrawable(drawable);
            }
        });

        //而通常指定subscribeOn(Scheduler.io()) 和 observeOn(AndroidSchedulers.mainThread())
        Observable.just(1,2,3,4)
                .subscribeOn(Schedulers.io())// 指定 subscribe() 发生在 IO 线程, 即事件生产的线程
                .observeOn(AndroidSchedulers.mainThread())// 指定 Subscriber 的回调发生在主线程，即事件消费的线程
                .subscribe(new Action1<Integer>() {
            @Override
            public void call(Integer number) {
                Log.d("jbo", "number: " + number);
            }
        });

        //---------------------map----------------------
        /*
        可以看到，map() 方法将参数中的 String 对象转换成一个 Bitmap 对象后返回，
        而在经过 map() 方法后，事件的参数类型也由 String 转为了 Bitmap。
        这种直接变换对象并返回的，是最常见的也最容易理解的变换。
        不过 RxJava 的变换远不止这样，它不仅可以针对事件对象，还可以针对整个事件队列，
        这使得 RxJava 变得非常灵活
         */
        Observable.just("images/logo.png")
                .map(new Func1<String, Bitmap>() {
                    @Override
                    public Bitmap call(String filePath) {
                        return getBitmapFromPath(filePath);
                    }
                })
                .subscribe(new Action1<Bitmap>() {
                    @Override
                    public void call(Bitmap bitmap) {
                        showBitmap(bitmap);
                    }
                });

        //数据为空请忽略就好，只实现流程
        final Student[] students = new Student[]{new Student(), new Student(), new Student()};
        //--------------------------打印名字----------------------------
        Subscriber<String> subscriber = new Subscriber<String>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(String name) {
                Log.d("jbo", name);
            }
        };

        Observable.from(students)
                .map(new Func1<Student, String>() {
                    @Override
                    public String call(Student student) {
                        return student.getName();
                    }
                })
                .subscribe(subscriber);

        //-----------------打印课程---------------------------------
        Subscriber<Student> subcriber2 = new Subscriber<Student>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Student student) {
                List<Course> courses = student.getCourses();
                for(int i = 0; i< courses.size(); i++) {
                    Course course = courses.get(i);
                    Log.d("jbo", course.getName());
                }
            }
        };

        Observable.from(students).subscribe(subcriber2);

        //------------------------flatmap--------------------
        /*
        那么如果我不想在 Subscriber 中使用 for 循环，
        而是希望 Subscriber 中直接传入单个的 Course 对象呢（这对于代码复用很重要）？
        用 map() 显然是不行的，因为 map() 是一对一的转化，而我现在的要求是一对多的转化。
        那怎么才能把一个 Student 转化成多个 Course 呢？
        这个时候，就需要用 flatMap() 了：
         */
        Subscriber<Course> subscriber3 = new Subscriber<Course>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Course course) {
                Log.d("jbo", course.getName());
            }
        };
        Observable.from(students)
                .flatMap(new Func1<Student, Observable<Course>>() {
                    @Override
                    public Observable<Course> call(Student student) {
                        return Observable.from(student.getCourses());
                    }
                }).subscribe(subscriber3);
        /*
        从上面的代码可以看出， flatMap() 和 map() 有一个相同点：
        它也是把传入的参数转化之后返回另一个对象。
        但需要注意，和 map() 不同的是，
        flatMap() 中返回的是个 Observable 对象，
        并且这个 Observable 对象并不是被直接发送到了 Subscriber 的回调方法中.

        flatMap() 的原理是这样的：
        1. 使用传入的事件对象创建一个 Observable 对象；
        2. 并不发送这个 Observable, 而是将它激活，于是它开始发送事件；
        3. 每一个创建出来的 Observable 发送的事件，都被汇入同一个 Observable ，而这个 Observable 负责将这些事件统一交给 Subscriber 的回调方法。
         */


        /*
        扩展：由于可以在嵌套的 Observable 中添加异步代码， flatMap() 也常用于嵌套的异步操作，例如嵌套的网络请求。
        示例代码（Retrofit + RxJava）：

        networkClient.token() // 返回 Observable<String>，在订阅时请求 token，并在响应后发送 token
                .flatMap(new Func1<String, Observable<Messages>>() {
                    @Override
                    public Observable<Messages> call(String token) {
                        // 返回 Observable<Messages>，在订阅时请求消息列表，并在响应后发送请求到的消息列表
                        return networkClient.messages();
                    }
                })
                .subscribe(new Action1<Messages>() {
                    @Override
                    public void call(Messages messages) {
                        // 处理显示消息列表
                        showMessages(messages);
                    }
                });

        传统的嵌套请求需要使用嵌套的 Callback 来实现。
        而通过 flatMap() ，可以把嵌套的请求写在一条链中，从而保持程序逻辑的清晰。
         */

        //--------------------lift------------------

        /*
        讲述 lift() 的原理只是为了让你更好地了解 RxJava ，从而可以更好地使用它。
        然而不管你是否理解了 lift() 的原理，RxJava 都不建议开发者自定义 Operator 来直接使用 lift()，
        而是建议尽量使用已有的 lift() 包装方法（如 map() flatMap() 等）进行组合来实现需求，
        因为直接使用 lift() 非常容易发生一些难以发现的错误。
         */
        final Subscriber<String> subscriber4 = new Subscriber<String>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(String s) {

            }
        };

        Observable.just(1,2,3,4).lift(new Observable.Operator<String, Integer>() {
            @Override
            public Subscriber<? super Integer> call(final Subscriber<? super String> subscriber) {
                //将事件序列中的Integer对象转换为String对象
                return new Subscriber<Integer>() {
                    @Override
                    public void onCompleted() {
                        subscriber4.onCompleted();
                    }

                    @Override
                    public void onError(Throwable e) {
                        subscriber4.onError(e);
                    }

                    @Override
                    public void onNext(Integer integer) {
                        subscriber4.onNext("" + integer);
                    }
                };
            }
        });


        /*
        前面讲到了，可以利用 subscribeOn() 结合 observeOn() 来实现线程控制，
        让事件的产生和消费发生在不同的线程。可是在了解了 map() flatMap() 等变换
        方法后，有些好事的（其实就是当初刚接触 RxJava 时的我）就问了：能不能多切换
        几次线程？

        答案是：能。因为 observeOn() 指定的是 Subscriber 的线程，而这个
        Subscriber 并不是（严格说应该为『不一定是』，但这里不妨理解为『不是』）
        subscribe() 参数中的 Subscriber ，而是 observeOn() 执行时的当前
        Observable 所对应的 Subscriber ，即它的直接下级 Subscriber 。
        换句话说，observeOn() 指定的是它之后的操作所在的线程。因此如果有多次切换
        线程的需求，只要在每个想要切换线程的位置调用一次 observeOn() 即可。
        上代码：
        */
        Observable.just(1, 2, 3, 4) // IO 线程，由 subscribeOn() 指定
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.newThread())
                .map(new Func1<Integer, Object>() {
                    @Override
                    public Object call(Integer integer) {
                        return null;
                    }
                }) // 新线程，由 observeOn() 指定
                .observeOn(Schedulers.io())
                .map(new Func1<Object, Object>() {
                    @Override
                    public Object call(Object o) {
                        return null;
                    }
                }) // IO 线程，由 observeOn() 指定
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Object>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Object o) {

                    }
                });  // Android 主线程，由 observeOn() 指定

    }

    class Student {
        String name;
        List<Course> courses;

        public String getName() {
            return name;
        }

        public List<Course> getCourses() {
            return courses;
        }
    }

    class Course {
        String name;
        public String getName() {
            return name;
        }
    }



    private void showBitmap(Bitmap bitmap) {
        //显示bitmap
    }

    private Bitmap getBitmapFromPath(String filePath) {
        //从图片路径里取得bitmap
        return null;
    }
}
