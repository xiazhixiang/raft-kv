package org.nuist.rpc.common.rpc;

/**
 * @description 服务审核状态
 */
public enum ServiceReviewState {
	
	HAS_NOT_REVIEWED, //未审核
	PASS_REVIEW,      //通过审核
	NOT_PASS_REVIEW,  //未通过审核
	FORBIDDEN         //禁用
	

}
