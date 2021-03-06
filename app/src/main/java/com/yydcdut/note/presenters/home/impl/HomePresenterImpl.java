package com.yydcdut.note.presenters.home.impl;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.yydcdut.note.aspect.permission.AspectPermission;
import com.yydcdut.note.bus.CategoryCreateEvent;
import com.yydcdut.note.bus.CategoryDeleteEvent;
import com.yydcdut.note.bus.CategoryEditEvent;
import com.yydcdut.note.bus.CategoryMoveEvent;
import com.yydcdut.note.bus.CategoryUpdateEvent;
import com.yydcdut.note.bus.PhotoNoteCreateEvent;
import com.yydcdut.note.bus.PhotoNoteDeleteEvent;
import com.yydcdut.note.bus.UserImageEvent;
import com.yydcdut.note.entity.Category;
import com.yydcdut.note.injector.ContextLife;
import com.yydcdut.note.model.compare.ComparatorFactory;
import com.yydcdut.note.model.rx.RxCategory;
import com.yydcdut.note.model.rx.RxPhotoNote;
import com.yydcdut.note.model.rx.RxUser;
import com.yydcdut.note.presenters.home.IHomePresenter;
import com.yydcdut.note.utils.Const;
import com.yydcdut.note.utils.FilePathUtils;
import com.yydcdut.note.utils.ImageManager.ImageLoaderManager;
import com.yydcdut.note.utils.PermissionUtils;
import com.yydcdut.note.utils.YLog;
import com.yydcdut.note.utils.permission.Permission;
import com.yydcdut.note.views.IView;
import com.yydcdut.note.views.home.IHomeView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import javax.inject.Inject;

import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by yuyidong on 15/11/19.
 */
public class HomePresenterImpl implements IHomePresenter {
    private IHomeView mHomeView;
    /**
     * 当前的category的Id
     */
    private int mCategoryId = -1;

    private RxCategory mRxCategory;
    private RxPhotoNote mRxPhotoNote;
    private RxUser mRxUser;
    private Context mContext;
    private Activity mActivity;

    @Inject
    public HomePresenterImpl(@ContextLife("Activity") Context context, Activity activity, RxCategory rxCategory,
                             RxPhotoNote rxPhotoNote, RxUser rxUser) {
        mContext = context;
        mRxCategory = rxCategory;
        mRxPhotoNote = rxPhotoNote;
        mRxUser = rxUser;
        mActivity = activity;
    }

    @Override
    public Context getContext() {
        return mContext;
    }

    @Override
    public void attachView(IView iView) {
        mHomeView = (IHomeView) iView;
        EventBus.getDefault().register(this);
        initDelay();
    }

    @Override
    public void detachView() {
        EventBus.getDefault().unregister(this);
    }

    public void setCategoryId(int categoryId) {
        mCategoryId = categoryId;
    }

    @Override
    public int getCategoryId() {
        return mCategoryId;
    }

    @Override
    public void setCheckCategoryPosition() {
        //todo
        mRxCategory.getAllCategories()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(categories -> {
                    boolean checkSuccessful = false;
                    for (int i = 0; i < categories.size(); i++) {
                        if (categories.get(i).isCheck()) {
                            mHomeView.setCheckPosition(i);
                            checkSuccessful = true;
                            break;
                        }
                    }
                    if (!checkSuccessful) {
                        mHomeView.setCheckPosition(0);
                    }
                }, (throwable -> YLog.e(throwable)));
    }

    @Override
    public void setCheckedCategoryPosition(int position) {
        mRxCategory.getAllCategories()
                .subscribe(categories -> {
                    mRxCategory.setCategoryMenuPosition(categories.get(position).getId())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(categories1 -> {
                                mHomeView.notifyCategoryDataChanged();
                                mCategoryId = categories1.get(position).getId();
                                mHomeView.changeFragment(mCategoryId);
                            }, (throwable -> YLog.e(throwable)));
                }, (throwable -> YLog.e(throwable)));
    }

    @Override
    public void changeCategoryAfterSaving(Category category) {
        mRxCategory.setCategoryMenuPosition(category.getId())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(categories -> {
                    mHomeView.notifyCategoryDataChanged();
                    mCategoryId = category.getId();
                    mHomeView.changePhotos4Category(mCategoryId);
                }, (throwable -> YLog.e(throwable)));
    }

    @Override
    public void setAdapter() {
        mRxCategory.getAllCategories()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(categories -> mHomeView.setCategoryList(categories), (throwable -> YLog.e(throwable)));
    }

    @Override
    public void drawerUserClick(int which) {
        switch (which) {
            case USER_ONE:
                mRxUser.isLoginQQ()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(aBoolean -> {
                            if (aBoolean) {
                                mHomeView.jump2UserCenterActivity();
                            } else {
                                mHomeView.jump2LoginActivity();
                            }
                        }, (throwable -> YLog.e(throwable)));
                break;
            case USER_TWO:
                mRxUser.isLoginEvernote()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(aBoolean -> {
                            if (aBoolean) {
                                mHomeView.jump2UserCenterActivity();
                            } else {
                                mHomeView.jump2LoginActivity();
                            }
                        }, (throwable -> YLog.e(throwable)));
                break;
        }
    }

    @Override
    public void drawerCloudClick() {
        mHomeView.cloudSyncAnimation();
    }

    @Override
    public void updateQQInfo() {
        mRxUser.isLoginQQ()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aBoolean -> {
                    if (aBoolean) {
                        mRxUser.getQQ()
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(iUser -> mHomeView.updateQQInfo(true, iUser.getName(), iUser.getImagePath()));
                    } else {
                        mHomeView.updateQQInfo(false, null, null);
                    }
                }, (throwable -> YLog.e(throwable)));
    }

    @Override
    public void updateEvernoteInfo() {
        mRxUser.isLoginEvernote()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aBoolean -> {
                    if (aBoolean) {
                        mHomeView.updateEvernoteInfo(true);
                    } else {
                        mHomeView.updateEvernoteInfo(false);
                    }
                }, (throwable -> YLog.e(throwable)));

    }

    @Override
    public void updateFromBroadcast(boolean broadcast_process, boolean broadcast_service) {
        //有时候categoryLabel为null，感觉原因是activity被回收了，但是一直解决不掉，所以迫不得已的解决办法
        if (mCategoryId == -1) {
            mRxCategory.getAllCategories()
                    .subscribe(categories -> {
                        for (Category category : categories) {
                            if (category.isCheck()) {
                                mCategoryId = category.getId();
                            }
                        }
                    }, (throwable -> YLog.e(throwable)));
        }

        //从另外个进程过来的数据
        if (broadcast_process) {
            mRxPhotoNote.refreshByCategoryId(mCategoryId, ComparatorFactory.FACTORY_NOT_SORT)
                    .subscribe(photoNoteList -> {
                        mRxCategory.findByCategoryId(mCategoryId)
                                .subscribe(category -> {
                                    category.setPhotosNumber(photoNoteList.size());
                                    mRxCategory.updateCategory(category)
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe(categories -> {
                                                mHomeView.notifyCategoryDataChanged();
                                            }, (throwable -> YLog.e(throwable)));
                                }, (throwable -> YLog.e(throwable)));
                    });
        }

        //从Service中来
        if (broadcast_service) {
            mRxCategory.getAllCategories()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(categories -> mHomeView.updateCategoryList(categories),
                            (throwable -> YLog.e(throwable)));
        }
    }

    @Override
    public void killCameraService() {
        Intent intent = new Intent(Const.BROADCAST_CAMERA_SERVICE_KILL);
        mContext.sendBroadcast(intent);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCategoryCreateEvent(CategoryCreateEvent categoryCreateEvent) {
        mRxCategory.getAllCategories()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(categories -> mHomeView.updateCategoryList(categories), (throwable -> YLog.e(throwable)));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCategoryUpdateEvent(CategoryUpdateEvent categoryUpdateEvent) {
        mRxCategory.getAllCategories()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(categories -> mHomeView.updateCategoryList(categories), (throwable -> YLog.e(throwable)));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCategoryMoveEvent(CategoryMoveEvent categoryMoveEvent) {
        mRxCategory.getAllCategories()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(categories -> mHomeView.updateCategoryList(categories), (throwable -> YLog.e(throwable)));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCategoryRenameEvent(CategoryEditEvent categoryEditEvent) {
        mRxCategory.getAllCategories()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(categories -> mHomeView.updateCategoryList(categories), (throwable -> YLog.e(throwable)));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCategoryDeleteEvent(CategoryDeleteEvent categoryDeleteEvent) {
        mRxCategory.getAllCategories()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(categories -> {
                    int beforeCategoryId = mCategoryId;
                    for (Category category : categories) {
                        if (category.isCheck()) {
                            mCategoryId = category.getId();
                            break;
                        }
                    }
                    mHomeView.updateCategoryList(categories);
                    if (mCategoryId != beforeCategoryId) {
                        mHomeView.changePhotos4Category(mCategoryId);
                    }
                }, (throwable -> YLog.e(throwable)));

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPhotoNoteCreateEvent(PhotoNoteCreateEvent photoNoteCreateEvent) {
        mRxPhotoNote.findByCategoryId(mCategoryId, ComparatorFactory.FACTORY_NOT_SORT)
                .subscribe(photoNoteList -> {
                    mRxCategory.findByCategoryId(mCategoryId)
                            .subscribe(category -> {
                                category.setPhotosNumber(photoNoteList.size());
                                mRxCategory.updateCategory(category)
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(categories -> {
                                            mHomeView.updateCategoryList(categories);
                                        }, (throwable -> YLog.e(throwable)));
                            }, (throwable -> YLog.e(throwable)));
                });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPhotoNoteDeleteEvent(PhotoNoteDeleteEvent photoNoteDeleteEvent) {
        mRxPhotoNote.findByCategoryId(mCategoryId, ComparatorFactory.FACTORY_NOT_SORT)
                .subscribe(photoNoteList -> {
                    mRxCategory.findByCategoryId(mCategoryId)
                            .subscribe(category -> {
                                category.setPhotosNumber(photoNoteList.size());
                                mRxCategory.updateCategory(category)
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(categories -> {
                                            mHomeView.updateCategoryList(categories);
                                        }, (throwable -> YLog.e(throwable)));
                            }, (throwable -> YLog.e(throwable)));
                }, (throwable -> YLog.e(throwable)));
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onGetUserImageEvent(UserImageEvent userImageEvent) {
        mRxUser.getQQ()
                .subscribe(iUser -> {
                    if (iUser != null) {
                        FilePathUtils.saveImage(FilePathUtils.getQQImagePath(),
                                ImageLoaderManager.loadImageSync(iUser.getNetImagePath()));
                    }
                }, (throwable -> YLog.e(throwable)));
    }

    /**
     * 因为相册页面的fragment和activity很沉重，速度之慢，所以这些不是特别必须的就稍后初始化
     */
    private void initDelay() {
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                initBaiduMap();
//            }
//        }, 200);
    }

    @Permission(PermissionUtils.CODE_PHONE_STATE)
    @AspectPermission(PermissionUtils.CODE_PHONE_STATE)
    private void initBaiduMap() {
//        SDKInitializer.initialize(mActivity.getApplication());
    }

}
