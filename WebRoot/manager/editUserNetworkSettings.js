function EditUserNetworkSettingsPageInit()
{
	console.log("EditUserNetworkSettingsPageInit()");
	EnterKeyListenerForEditUserNetworkSettings();
}

function showEditUserNetworkSettingsModal(text){
	$(".editUserNetworkSettingsModal").fadeIn("slow");
	$("#networkSettings").focus();
}

function closeEditUserNetworkSettingsModal(){
	$(".editUserNetworkSettingsModal").fadeOut("slow");
}

//回车键监听函数
function EnterKeyListenerForEditUserNetworkSettings(){
	console.log("start enter key listener");
	var event=arguments.callee.caller.arguments[0]||window.event;//消除浏览器差异  
 	if (event.keyCode == 13){  
 		editUserNetworkSettings();
 	}  
}

function editUserNetworkSettings(){
	console.log("editUserNetworkSettings()");

	var networkSettings = $("#networkSettings").val();
    
    console.log("editUserNetworkSettings()",id, networkSettings);
    
    $.ajax({
        url : "/DocSystem/Manage/editUserNetworkSettings.do",
        type : "post",
        dataType : "json",
        data : {
        	 id : id,
             name : name,
             networkSettings : networkSettings,
        },
        success : function (ret) {
            if( "ok" == ret.status ){
            	showUserList(gPageIndex);	//刷新UserList
            	alert(_Lang("更新成功"));
            }else {
            	alert(_Lang("更新失败", ":", ret.msgInfo));
            }
        },
        error : function () {
        	alert(_Lang("更新失败", ":", "服务器异常"));
        }
    });
}

//页面初始化代码    
$(function(){
	console.log("editUserNetworkSettings Page init");
	EditUserNetworkSettingsPageInit();
	$("#name").click().focus();
});