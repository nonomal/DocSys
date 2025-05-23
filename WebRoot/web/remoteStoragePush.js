function remoteStoragePushPageInit(_node, _repos)
{
	console.log("remoteStoragePushPageInit() ", _node, _repos);
	RemoteStoragePush.init(_node, _repos);  
}

function closeRemoteStoragePushDialog()
{
	closeBootstrapDialog("remoteStoragePush");
}

function doRemoteStoragePush()
{
	RemoteStoragePush.doRemoteStoragePush();
  	closeRemoteStoragePushDialog();	
}

function cancelRemoteStoragePush()
{
	closeRemoteStoragePushDialog();
}

function doSelectRecurciveConfirm()
{
	var recurciveEn = $("#dialog-remoteStoragePush input[name='recurciveEn']").is(':checked')? 1: 0;
	if(recurciveEn == 1)
	{
		qiao.bs.confirm({
	        id: 'recurcivePullConfirm',
	        title: _Lang("确认操作"),
	        okbtn: _Lang("确认"),
	        qubtn: _Lang("取消"),
	        msg: _Lang('该操作将推送目录下的所有文件，是否允许？'),
	    },function(){
	    	//确认
	    	$("#dialog-remoteStoragePush input[name='recurciveEn']").attr("checked","checked");
	    },function(){
			//取消
	    	$("#dialog-remoteStoragePush input[name='recurciveEn']").attr("checked",false);			
	    });
	}
}


function doSelectForceConfirm()
{
	var forceEn = $("#dialog-remoteStoragePush input[name='forceEn']").is(':checked')? 1: 0;
	if(forceEn == 1)
	{
		qiao.bs.confirm({
	        id: 'forcePullConfirm',
	        title: _Lang("确认操作"),
	        okbtn: _Lang("确认"),
	        qubtn: _Lang("取消"),
	        msg: _Lang('远程文件改动将被强制覆盖，是否强制推送？'),
	    },function(){
	    	//确认
	    	$("#dialog-remoteStoragePush input[name='forceEn']").attr("checked","checked");
	    },function(){
			//取消
	    	$("#dialog-remoteStoragePush input[name='forceEn']").attr("checked",false);			
	    });
	}
}

function doSelectDeleteConfirm()
{
	var deleteEn = $("#dialog-remoteStoragePush input[name='deleteEn']").is(':checked')? 1: 0;
	if(deleteEn == 1)
	{
		qiao.bs.confirm({
	        id: 'deletePullConfirm',
	        title: _Lang("确认操作"),
	        okbtn: _Lang("确认"),
	        qubtn: _Lang("取消"),
	        msg: _Lang('远程文件可能被删除，是否允许删除？'),
	    },function(){
	    	//确认
	    	$("#dialog-remoteStoragePush input[name='deleteEn']").attr("checked","checked");
	    },function(){
			//取消
	    	$("#dialog-remoteStoragePush input[name='deleteEn']").attr("checked",false);			
	    });
	}
}

var RemoteStoragePush = (function () {
	var node;
	var repos;
	
	function init(_node, _repos)
	{
		console.log("RemoteStoragePush.init() node:", _node, _repos);

		node = _node;
		repos = _repos;
		
		var targetServer = getTargetServerDispInfo(repos);
        $("#dialog-remoteStoragePush input[name='targetServer']").val(targetServer);

		if(node.type == 2)
		{
			$("#dialog-remoteStoragePush span[name='recurciveEnSpan']").show();				
		}
					
		//set pushEntryPath
		var pushEntryPath = node.path;
		if(pushEntryPath == undefined || pushEntryPath == "")
		{
			pushEntryPath = "/";
		}
		if(node.name)
		{
			pushEntryPath += node.name;	
		}
        $("#dialog-remoteStoragePush input[name='pushEntryPath']").val(pushEntryPath);
	}
	
	function doRemoteStoragePush()
	{		   	   
	    remoteStoragePush();
	    return true;
	}
	
   	function remoteStoragePush()
   	{
   		var recurciveEn = $("#dialog-remoteStoragePush input[name='recurciveEn']").is(':checked')? 1: 0;
   		var forceEn = $("#dialog-remoteStoragePush input[name='forceEn']").is(':checked')? 1: 0;
   		var deleteEn = $("#dialog-remoteStoragePush input[name='deleteEn']").is(':checked')? 1: 0;
   		
    	$.ajax({
             url : "/DocSystem/Bussiness/remoteStoragePush.do",
             type : "post",
             dataType : "json",
             data : {
                reposId : repos.id, 
                path: node.path,
                name : node.name,
	            shareId: gShareId,
	            recurciveEn : recurciveEn,
	            forceEn : forceEn,
	            deleteEn : deleteEn,
             },
             success : function (ret) {
            	console.log("remoteStoragePush ret:", ret);            		
             	if( "ok" == ret.status){             		
             		// 普通消息提示条
             		showPushResultInfo(ret);     		
                }
                else
                {
                	showErrorMessage(_Lang("推送失败", " : ", ret.msgInfo));
                }
            },
            error : function () {
                showErrorMessage(_Lang("推送失败", " : ", "服务器异常"));
            }
        });
    }
   	
   	function showPushResultInfo(ret)
   	{
   		var totalNum = ret.dataEx.totalCount;
   		var successNum = ret.dataEx.successCount;
		var pushResultInfo = "";
		switch(langType)
		{
		case "en":
			pushResultInfo = "Push Completed(Total : " + totalNum + ")";	
			break;
		default:
			pushResultInfo = "推送成功(共" + totalNum +"个)";
			break;
		}
  		if(successNum != totalNum)
  		{
  			switch(langType)
  			{
  			case "en":
  				pushResultInfo = "Push Completed(Total : " + totalNum + ", Failed : " + (totalNum - successNum) + ")";
  				break;
  			default:
  	  			pushResultInfo = "推送完成 (共" + totalNum +"个)"+",成功 " + successNum + "个";
  				break;
  			}

  			// 普通消息提示条
			bootstrapQ.msg({
					msg : pushResultInfo,
					type : 'warning',
					time : 2000,
				    }); 
  		}
  		else
  		{
            // 普通消息提示条
			bootstrapQ.msg({
					msg : pushResultInfo,
					type : 'success',
					time : 2000,
				    }); 
  		}
   	}
   	
	//开放给外部的调用接口
    return {
		init: function(_node, _repos){
			init(_node, _repos);
		},
    	doRemoteStoragePush: function(){
    		doRemoteStoragePush();
        },
    };
})();
