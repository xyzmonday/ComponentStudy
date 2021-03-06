package com.richfit.data.repository;

import android.util.Log;

import com.richfit.data.constant.Global;
import com.richfit.data.helper.CommonUtil;
import com.richfit.domain.bean.BizFragmentConfig;
import com.richfit.domain.bean.ImageEntity;
import com.richfit.domain.bean.InvEntity;
import com.richfit.domain.bean.InventoryEntity;
import com.richfit.domain.bean.LoadBasicDataWrapper;
import com.richfit.domain.bean.LoadDataTask;
import com.richfit.domain.bean.MaterialEntity;
import com.richfit.domain.bean.MenuNode;
import com.richfit.domain.bean.RefNumEntity;
import com.richfit.domain.bean.ReferenceEntity;
import com.richfit.domain.bean.ResultEntity;
import com.richfit.domain.bean.RowConfig;
import com.richfit.domain.bean.SimpleEntity;
import com.richfit.domain.bean.UpdateEntity;
import com.richfit.domain.bean.UserEntity;
import com.richfit.domain.bean.WorkEntity;
import com.richfit.domain.repository.ILocalRepository;
import com.richfit.domain.repository.IServerRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Flowable;

/**
 * 数据仓库，该仓库管理了远程数据，本地数据（数据库，SharePreference），
 * 文件和内存缓存
 * Created by monday on 2016/12/29.
 */

public final class Repository implements ILocalRepository, IServerRepository {


    private ILocalRepository mLocalRepository;

    private IServerRepository mServerRepository;

    private boolean isLocal;


    private Repository() {
    }

    Repository(IServerRepository serverRepository, ILocalRepository localRepository) {
        this.mLocalRepository = localRepository;
        this.mServerRepository = serverRepository;
    }

    public boolean isLocal() {
        return isLocal;
    }

    public void setLocal(boolean local) {
        isLocal = local;
        Log.e("yff", "isLocal = " + isLocal);
    }

    @Override
    public Flowable<UserEntity> login(String userName, String password) {
        return isLocal ? mLocalRepository.login(userName, password) : mServerRepository.login(userName, password);
    }

    /**
     * 2017年06月09日修改，对于离线如果没有在本地没有查到返回error,如果是在线，那么先从本地获取然后
     * 在从网络获取，加快响应速度
     *
     * @param loginId
     * @param mode
     * @return
     */
    @Override
    public Flowable<ArrayList<MenuNode>> getMenuInfo(String loginId, int mode) {
        return isLocal ? mLocalRepository.getMenuInfo(loginId, mode)
                .flatMap(list -> {
                    if (list == null || list.size() == 0) {
                        return Flowable.error(new Throwable("未获取到用户菜单"));
                    }
                    return Flowable.just(list);
                }) : mServerRepository.getMenuInfo(loginId, mode);

//                Flowable.concat(mLocalRepository.getMenuInfo(loginId, mode),
//                        mServerRepository.getMenuInfo(loginId, mode))
//                        .filter(list -> list != null && list.size() > 0)
//                        .firstOrError()
//                        .toFlowable();

    }

    @Override
    public Flowable<ArrayList<RowConfig>> loadExtraConfig(String companyId) {
        return isLocal ? mLocalRepository.loadExtraConfig(companyId) : mServerRepository.loadExtraConfig(companyId);
    }

    @Override
    public Flowable<String> getConnectionStatus() {
        return mServerRepository.getConnectionStatus();
    }

    @Override
    public Flowable<List<ReferenceEntity>> getArrivalInfo(String createdBy, String creationDate, Map<String, Object> extraMap) {
        return mServerRepository.getArrivalInfo(createdBy, creationDate, extraMap);
    }

    @Override
    public Flowable<ArrayList<BizFragmentConfig>> loadBizFragmentConfig(String companyId, int mode) {
        return null;
    }

    /**
     * 下载基础数据的总条数
     *
     * @param param:需要下载的基础数据的类型
     * @return
     */
    @Override
    public Flowable<LoadBasicDataWrapper> preparePageLoad(LoadBasicDataWrapper param) {
        return mServerRepository.preparePageLoad(param);
    }

    @Override
    public Flowable<String> resetPassword(String userId, String password) {
        return mServerRepository.resetPassword(userId, password);
    }

    /**
     * 下载基础数据。对于增量更新的数据（仓位和物料）那么需要保存当前请求的日期，
     * 对于非增量更新的数据，那么使用当前日期即可。
     *
     * @param task
     * @return
     */
    @Override
    public Flowable<List<Map<String, Object>>> loadBasicData(final LoadDataTask task) {

        final String queryType = task.queryType;
        final String currentDate = CommonUtil.getCurrentDate(Global.GLOBAL_DATE_PATTERN_TYPE4);
        String queryDate = currentDate;
        //针对增量更新的基础数据，使用上一次请求的日期
        switch (queryType) {
            case "WL":
            case "CW":
            case "CC":
            case "XM":
                queryDate = getLoadBasicDataTaskDate(queryType);
                break;
        }
        //保存查询日期
        task.queryDate = queryDate;
        final Map<String, Object> tmp = new HashMap<>();
        tmp.put("queryType", queryType);
        tmp.put("queryDate", queryDate);
        tmp.put("isFirstPage", task.isFirstPage);
        tmp.put("taskId", task.id);
        return mServerRepository.loadBasicData(task)
                .filter(list -> list != null && list.size() > 0)
                .zipWith(Flowable.just(tmp), (maps, map) -> {
                    maps.add(0, map);
                    return maps;
                }).onBackpressureBuffer();

    }

    @Override
    public Flowable<String> syncDate() {
        return mServerRepository.syncDate();
    }

    @Override
    public Flowable<String> getMappingInfo() {
        return mServerRepository.getMappingInfo();
    }


    @Override
    public Flowable<Map<String, List<SimpleEntity>>> getDictionaryData(String... codes) {
        return mLocalRepository.getDictionaryData(codes);
    }

    @Override
    public Flowable<ArrayList<String>> readUserInfo(String userName, String password) {
        return mLocalRepository.readUserInfo(userName, password);
    }

    @Override
    public void saveUserInfo(UserEntity userEntity) {
        mLocalRepository.saveUserInfo(userEntity);
    }

    /**
     * 获取基础数据下载的日期
     *
     * @param requestType：下载基础数据的请求类型
     * @return
     */
    @Override
    public String getLoadBasicDataTaskDate(String requestType) {
        return mLocalRepository.getLoadBasicDataTaskDate(requestType);
    }

    @Override
    public Flowable<String> uploadCollectionDataSingle(ResultEntity result) {
        return isLocal ? mLocalRepository.uploadCollectionDataSingle(result) :
                mServerRepository.uploadCollectionDataSingle(result);
    }

    @Override
    public Flowable<String> uploadCollectionData(String refCodeId, String transId, String bizType,
                                                 String refType, int inspectionType, String voucherDate,
                                                 String remark, String userId, Map<String, Object> extraHeaderMap) {
        return mServerRepository.uploadCollectionData(refCodeId, transId, bizType, refType, inspectionType,
                voucherDate, remark, userId, extraHeaderMap);
    }

    @Override
    public Flowable<String> transferCollectionData(ResultEntity result) {
        return mServerRepository.transferCollectionData(result);
    }

    @Override
    public Flowable<String> transferCollectionData(String transId, String bizType, String refType, String userId, String voucherDate,
                                                   String transToSAPFlag, Map<String, Object> extraHeaderMap) {
        return mServerRepository.transferCollectionData(transId, bizType, refType, userId, voucherDate, transToSAPFlag, extraHeaderMap);
    }

    @Override
    public Flowable<List<RefNumEntity>> getReserveNumList(String beginDate, String endDate, String loginId, String refType) {
        return mServerRepository.getReserveNumList(beginDate, endDate, loginId, refType);
    }

    @Override
    public Flowable<String> uploadInspectionImage(ResultEntity result) {
        return mServerRepository.uploadInspectionImage(result);
    }

    @Override
    public Flowable<String> uploadCheckDataSingle(ResultEntity result) {
        return isLocal ? mLocalRepository.uploadCheckDataSingle(result) :
                mServerRepository.uploadCheckDataSingle(result);
    }

    @Override
    public Flowable<List<InventoryEntity>> getInventoryInfo(String queryType, String workId, String invId,
                                                            String workCode, String invCode, String storageNum,
                                                            String materialNum, String materialId, String materialGroup,
                                                            String materialDesc, String batchFlag,
                                                            String location, String specialInvFlag, String specialInvNum,
                                                            String invType, Map<String, Object> extraMap) {
        return mServerRepository.getInventoryInfo(queryType, workId, invId, workCode, invCode, storageNum, materialNum, materialId, materialGroup,
                materialDesc, CommonUtil.toUpperCase(batchFlag), CommonUtil.toUpperCase(location), specialInvFlag, specialInvNum, invType, extraMap);
    }

    @Override
    public Flowable<String> getLocationInfo(String queryType, String workId, String invId,
                                            String storageNum, String location, Map<String, Object> extraMap) {
        return isLocal ? mLocalRepository.getLocationInfo(queryType, workId, invId, storageNum, CommonUtil.toUpperCase(location), extraMap) :
                mServerRepository.getLocationInfo(queryType, workId, invId, storageNum, CommonUtil.toUpperCase(location), extraMap);

    }

    @Override
    public Flowable<UpdateEntity> getAppVersion() {
        return mServerRepository.getAppVersion();
    }

    /**
     * 获取单据数据，如果是离线则从本地数据库中获取
     *
     * @param refNum：单号
     * @param refType:单据类型
     * @param bizType：业务类型
     * @param moveType:移动类型
     * @param userId：用户loginId
     * @return
     */
    @Override
    public Flowable<ReferenceEntity> getReference(String refNum, String refType, String bizType, String moveType,
                                                  String refLineId, String userId) {
        return isLocal ? mLocalRepository.getReference(refNum, refType, bizType, moveType, refLineId, userId) :
                mServerRepository.getReference(refNum, refType, bizType, moveType, refLineId, userId);
    }

    /**
     * 抬头界面删除整单缓存数据
     *
     * @param refNum：单据号
     * @param transId：缓存id
     * @param refCodeId:单据抬头id
     * @param refType:单据类型
     * @param bizType:业务类型
     * @param userId：用户id
     * @return
     */
    @Override
    public Flowable<String> deleteCollectionData(String refNum, String transId, String refCodeId,
                                                 String refType, String bizType, String userId,
                                                 String companyCode) {
        return isLocal ? mLocalRepository.deleteCollectionData(refNum, transId, refCodeId,
                refType, bizType, userId, companyCode) : mServerRepository.deleteCollectionData(refNum, transId, refCodeId,
                refType, bizType, userId, companyCode);
    }

    /**
     * 获取整单缓存
     *
     * @param refCodeId：单据id
     * @param bizType:业务类型
     * @param refType：单据类型
     * @return
     */
    @Override
    public Flowable<ReferenceEntity> getTransferInfo(String recordNum, String refCodeId, String bizType, String refType, String userId,
                                                     String workId, String invId, String recWorkId, String recInvId, Map<String, Object> extraMap) {
        return isLocal ? mLocalRepository.getTransferInfo(recordNum, refCodeId, bizType, refType, userId,
                workId, invId, recWorkId, recInvId, extraMap) :
                mServerRepository.getTransferInfo(recordNum, refCodeId, bizType, refType, userId,
                        workId, invId, recWorkId, recInvId, extraMap);
    }

    @Override
    public Flowable<ReferenceEntity> getTransferInfoSingle(String refCodeId, String refType, String bizType, String refLineId,
                                                           String workId, String invId, String recWorkId, String recInvId,
                                                           String materialNum, String batchFlag, String location, String refDoc, int refDocItem, String userId) {
        return isLocal ? mLocalRepository.getTransferInfoSingle(refCodeId, refType, bizType, refLineId,
                workId, invId, recWorkId, recInvId, materialNum, CommonUtil.toUpperCase(batchFlag), CommonUtil.toUpperCase(location), refDoc, refDocItem, userId)
                : mServerRepository.getTransferInfoSingle(refCodeId, refType, bizType, refLineId,
                workId, invId, recWorkId, recInvId, materialNum, CommonUtil.toUpperCase(batchFlag), CommonUtil.toUpperCase(location), refDoc, refDocItem, userId);
    }


    @Override
    public Flowable<String> deleteCollectionDataSingle(String lineDeleteFlag, String transId, String transLineId,
                                                       String locationId, String refType, String bizType, String refLineId, String userId,
                                                       int position, String companyCode) {
        return isLocal ? mLocalRepository.deleteCollectionDataSingle(lineDeleteFlag, transId,
                transLineId, locationId, refType, bizType, refLineId, userId, position, companyCode) :
                mServerRepository.deleteCollectionDataSingle(lineDeleteFlag, transId,
                        transLineId, locationId, refType, bizType, refLineId, userId, position, companyCode);
    }

    @Override
    public Flowable<ReferenceEntity> getCheckInfo(String userId, String bizType, String checkLevel, String checkSpecial,
                                                  String storageNum, String workId, String invId, String checkNum,
                                                  String checkDate, Map<String, Object> extraMap) {
        return isLocal ? mLocalRepository.getCheckInfo(userId, bizType, checkLevel, checkSpecial, storageNum, workId, invId, checkNum, checkDate, extraMap) :
                mServerRepository.getCheckInfo(userId, bizType, checkLevel, checkSpecial, storageNum, workId, invId, checkNum, checkDate, extraMap);
    }

    @Override
    public Flowable<String> deleteCheckData(String storageNum, String workId, String invId, String checkId, String userId, String bizType) {
        return isLocal ? mLocalRepository.deleteCheckData(storageNum, workId, invId, checkId, userId, bizType) :
                mServerRepository.deleteCheckData(storageNum, workId, invId, checkId, userId, bizType);
    }

    @Override
    public Flowable<List<InventoryEntity>> getCheckTransferInfoSingle(String checkId, String materialId, String materialNum, String location, String bizType) {
        return isLocal ? mLocalRepository.getCheckTransferInfoSingle(checkId, materialId, materialNum, CommonUtil.toUpperCase(location), bizType)
                : mServerRepository.getCheckTransferInfoSingle(checkId, materialId, materialNum, CommonUtil.toUpperCase(location), bizType);
    }

    @Override
    public Flowable<ReferenceEntity> getCheckTransferInfo(String checkId, String materialNum, String location, String isPageQuery, int pageNum, int pageSize, String bizType) {
        return isLocal ? mLocalRepository.getCheckTransferInfo(checkId, materialNum, CommonUtil.toUpperCase(location), isPageQuery, pageNum, pageSize, bizType)
                : mServerRepository.getCheckTransferInfo(checkId, materialNum, CommonUtil.toUpperCase(location), isPageQuery, pageNum, pageSize, bizType);
    }

    @Override
    public Flowable<String> deleteCheckDataSingle(String checkId, String checkLineId, String userId, String bizType) {
        return isLocal ? mLocalRepository.deleteCheckDataSingle(checkId, checkLineId, userId, bizType)
                : mServerRepository.deleteCheckDataSingle(checkId, checkLineId, userId, bizType);
    }

    /**
     * 保存基础数据的下载日期
     *
     * @param queryTypes
     * @param requestDate
     */
    @Override
    public void saveLoadBasicDataTaskDate(String requestDate, List<String> queryTypes) {
        if (queryTypes.size() <= 0) {
            return;
        }
        //保存增量更新对应的基础数据的请求日期
        mLocalRepository.saveLoadBasicDataTaskDate(requestDate, queryTypes);
    }

    /**
     * 保存基础数据
     *
     * @param maps:基础数据源
     */
    @Override
    public Flowable<Integer> saveBasicData(List<Map<String, Object>> maps) {
        return mLocalRepository.saveBasicData(maps);
    }

    @Override
    public Flowable<ArrayList<InvEntity>> getInvsByWorkId(String workId, int flag) {
        return mLocalRepository.getInvsByWorkId(workId, flag);
    }

    @Override
    public Flowable<ArrayList<WorkEntity>> getWorks(int flag) {
        return mLocalRepository.getWorks(flag);
    }

    @Override
    public Flowable<Boolean> checkWareHouseNum(String sendWorkId, String sendInvCode, String recWorkId,
                                               String recInvCode, int flag) {
        return mLocalRepository.checkWareHouseNum(sendWorkId, sendInvCode, recWorkId, recInvCode, flag);
    }

    @Override
    public Flowable<Map<String, List<SimpleEntity>>> getAutoComList(String workCode, Map<String, Object> extraMap,
                                                                    String keyWord, int defaultItemNum, int flag, String... keys) {
        return mLocalRepository.getAutoComList(workCode, extraMap, keyWord, defaultItemNum, flag, keys);
    }


    @Override
    public Flowable<Boolean> saveBizFragmentConfig(ArrayList<BizFragmentConfig> bizFragmentConfigs) {
        return mLocalRepository.saveBizFragmentConfig(bizFragmentConfigs);
    }

    @Override
    public Flowable<ArrayList<BizFragmentConfig>> readBizFragmentConfig(String bizType, String refType, int fragmentType, int mode) {
        return mLocalRepository.readBizFragmentConfig(bizType, refType, fragmentType, mode);
    }

    @Override
    public void deleteInspectionImages(String refNum, String refCodeId, boolean isLocal) {
        mLocalRepository.deleteInspectionImages(refNum, refCodeId, isLocal);
    }

    @Override
    public void deleteInspectionImagesSingle(String refNum, String refLineNum, String refLineId, boolean isLocal) {
        mLocalRepository.deleteInspectionImagesSingle(refNum, refLineNum, refLineId, isLocal);
    }

    @Override
    public Flowable<String> deleteTakedImages(ArrayList<ImageEntity> images, boolean isLocal) {
        return mLocalRepository.deleteTakedImages(images, isLocal);
    }

    @Override
    public void saveTakedImages(ArrayList<ImageEntity> images, String refNum, String refLineId,
                                int takePhotoType, String imageDir, boolean isLocal) {
        mLocalRepository.saveTakedImages(images, refNum, refLineId, takePhotoType, imageDir, isLocal);
    }

    @Override
    public ArrayList<ImageEntity> readImagesByRefNum(String refNum, boolean isLocal) {
        return mLocalRepository.readImagesByRefNum(refNum, isLocal);
    }

    @Override
    public Flowable<String> getStorageNum(String workId, String workCode, String invId, String invCode) {
        return mLocalRepository.getStorageNum(workId, workCode, invId, invCode);
    }

    @Override
    public Flowable<ArrayList<String>> getStorageNumList(int flag) {
        return mLocalRepository.getStorageNumList(flag);
    }

    @Override
    public ArrayList<MenuNode> saveMenuInfo(ArrayList<MenuNode> menus, String loginId, int mode) {
        return mLocalRepository.saveMenuInfo(menus, loginId, mode);
    }

    @Override
    public void saveReferenceInfo(ReferenceEntity refData, String bizType, String refType) {
        mLocalRepository.saveReferenceInfo(refData, bizType, refType);
    }

    @Override
    public Flowable<ArrayList<MenuNode>> readMenuInfo(String loginId) {
        return mLocalRepository.readMenuInfo(loginId);
    }

    @Override
    public Flowable<String> changeLoginInfo(String userId, String newPassword) {
        return mServerRepository.changeLoginInfo(userId, newPassword);
    }

    @Override
    public Flowable<String> uploadMultiFiles(List<ResultEntity> results) {
        return mServerRepository.uploadMultiFiles(results);
    }

    @Override
    public Flowable<String> uploadMultiFilesOffline(List<ResultEntity> results) {
        return mServerRepository.uploadCheckDataOffline(results);
    }

    @Override
    public Flowable<ResultEntity> getDeviceInfo(String deviceId) {
        return mServerRepository.getDeviceInfo(deviceId);
    }

    @Override
    public Flowable<String> uploadCollectionDataOffline(List<ResultEntity> results) {
        return mServerRepository.uploadCollectionDataOffline(results);
    }

    @Override
    public Flowable<String> uploadCheckDataOffline(List<ResultEntity> results) {
        return mServerRepository.uploadCheckDataOffline(results);
    }

    @Override
    public Flowable<List<InventoryEntity>> getInspectionInfo(String bizType, String materialNum, String userId, String workCode, Map<String, Object> extraMap) {
        return mServerRepository.getInspectionInfo(bizType, materialNum, userId, workCode, extraMap);
    }

    @Override
    public Flowable<List<InventoryEntity>> getSuggestInventoryInfo(String workCode, String invCode, String materialNum, String queryType, Map<String, Object> extraMap) {
        return isLocal?mLocalRepository.getSuggestInventoryInfo(workCode, invCode, materialNum, queryType, extraMap):
        mServerRepository.getSuggestInventoryInfo(workCode, invCode, materialNum, queryType, extraMap);
    }

    @Override
    public Flowable<List<ReferenceEntity>> readTransferedData(int bizType) {
        return mLocalRepository.readTransferedData(bizType);
    }

    @Override
    public void deleteOfflineDataAfterUploadSuccess(String transId, String bizType, String refType, String userId) {
        mLocalRepository.deleteOfflineDataAfterUploadSuccess(transId, bizType, refType, userId);
    }

    @Override
    public Flowable<String> setTransFlag(String bizType, String transId, String transFlag) {
        return mLocalRepository.setTransFlag(bizType, transId, transFlag);
    }

    @Override
    public Flowable<String> uploadEditedHeadData(ResultEntity resultEntity) {
        return mLocalRepository.uploadEditedHeadData(resultEntity);
    }

    @Override
    public Flowable<List<String>> getLocationList(String workId, String workCode, String invId, String invCode, String keyWord, int defaultItemNum, int flag) {
        return mLocalRepository.getLocationList(workId, workCode, invId, invCode, keyWord, defaultItemNum, flag);
    }

    @Override
    public String getBatchManagerStatus(String workId, String materialId) {
        return mLocalRepository.getBatchManagerStatus(workId, materialId);
    }

    @Override
    public Flowable<MaterialEntity> getMaterialInfo(String queryType, String materialNum) {
        return isLocal ? mLocalRepository.getMaterialInfo(queryType, materialNum) : mServerRepository.getMaterialInfo(queryType, materialNum);
    }

    @Override
    public Flowable<String> transferCheckData(String checkId, String userId, String bizType) {
        return isLocal ? mLocalRepository.transferCheckData(checkId, userId, bizType) :
                mServerRepository.transferCheckData(checkId, userId, bizType);
    }

    @Override
    public Flowable<String> uploadCheckData(String checkId, String userId, String bizType) {
        return isLocal ? mLocalRepository.uploadCheckData(checkId, userId, bizType) :
                mServerRepository.uploadCheckData(checkId, userId, bizType);
    }
}
