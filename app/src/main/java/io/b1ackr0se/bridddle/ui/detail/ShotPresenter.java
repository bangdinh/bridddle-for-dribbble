package io.b1ackr0se.bridddle.ui.detail;

import javax.inject.Inject;

import io.b1ackr0se.bridddle.base.BasePresenter;
import io.b1ackr0se.bridddle.data.model.Like;
import io.b1ackr0se.bridddle.data.model.Shot;
import io.b1ackr0se.bridddle.data.remote.dribbble.DataManager;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;


public class ShotPresenter extends BasePresenter<ShotView> {
    public static final int PER_PAGE = 40;
    private int page = 1;
    private Shot shot;
    private CompositeSubscription compositeSubscription;
    private Subscription likeSubscription, unlikeSubscription;
    private DataManager dataManager;

    @Inject
    public ShotPresenter(DataManager dataManager) {
        this.compositeSubscription = new CompositeSubscription();
        this.dataManager = dataManager;
    }

    public void setShot(Shot shot) {
        this.shot = shot;
    }

    public void load() {
        getView().bind();
        loadComment(true);
    }

    public void loadComment(boolean firstPage) {

        if (shot == null) return;

        if (firstPage) {
            page = 1;
        }

        Subscription subscription = dataManager.getComments(shot.getId(), page, PER_PAGE)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(comments -> {
                    if (comments.isEmpty())
                        getView().showNoComment();
                    else {
                        page++;
                        getView().showComments(comments);
                    }
                }, throwable -> {
                    getView().showError();
                });

        compositeSubscription.add(subscription);

    }

    public void performLikeOrUnlike() {
        if (shot.isLiked()) unlike();
        else like();
    }

    public void checkLike() {
        if (!dataManager.isLoggedIn()) {
            getView().showLike(false);
            return;
        }

        if (shot == null) {
            getView().showLike(false);
            return;
        }

        getView().showLikeInProgress(true);

        Subscription subscription = dataManager.liked(shot.getId())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(like -> {
                    shot.setLiked(like != null);
                    getView().showLikeInProgress(false);
                    getView().showLike(like != null);
                }, throwable -> {
                    shot.setLiked(false);
                    getView().showLikeInProgress(false);
                    getView().showLike(false);
                });
        compositeSubscription.add(subscription);
    }

    public void like() {
        if (!dataManager.isLoggedIn()) {
            getView().showLike(false);
            getView().performLogin();
            return;
        }

        if (shot == null) return;

        getView().showLikeInProgress(true);

        if (unlikeSubscription != null) compositeSubscription.remove(unlikeSubscription);

        likeSubscription = dataManager.like(shot.getId())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(like -> {
                    shot.setLiked(true);
                    getView().showLikeInProgress(false);
                    getView().showLike(true);
                }, throwable -> {
                    getView().showLikeInProgress(false);
                    getView().showLike(false);
                    getView().failedToLike(true);
                });
        compositeSubscription.add(likeSubscription);

    }

    public void unlike() {
        if (!dataManager.isLoggedIn()) {
            getView().showLike(false);
            getView().performLogin();
            return;
        }

        if (shot == null) return;

        getView().showLikeInProgress(true);

        if (likeSubscription != null) compositeSubscription.remove(likeSubscription);

        unlikeSubscription = dataManager.unlike(shot.getId())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(e -> getView().failedToLike(false))
                .subscribe(aVoid -> {
                    shot.setLiked(false);
                    getView().showLikeInProgress(false);
                    getView().showLike(false);
                }, throwable -> {
                    getView().showLikeInProgress(false);
                    getView().showLike(true);
                    getView().failedToLike(false);
                });
        compositeSubscription.add(unlikeSubscription);
    }

    @Override
    public void detachView() {
        super.detachView();
        compositeSubscription.unsubscribe();
    }
}
