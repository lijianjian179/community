$(function(){
	$("#sendBtn").click(send_letter);
	$(".close").click(delete_msg);
});

function send_letter() {
	$("#sendModal").modal("hide");

    // 获取标题和内容
    var toName = $("#recipient-name").val();
    var content = $("#message-text").val();
	// 发送异步请求
    $.post(
        CONTEXT_PATH + "/letter/send",
        {"toName":toName,"content":content},
        function(data) {
            data = $.parseJSON(data);
//    	        if (data.code == 0) {
                $("#hintBody").text(data.msg);
//    	        } else {
//    	            $("#hintBody").text("发送成功！");
//    	        }

            // 显示提示框
            $("#hintModal").modal("show");
            // 2秒后，自动隐藏提示框
            setTimeout(function(){
                $("#hintModal").modal("hide");
                    window.location.reload();
            }, 2000);
        }
    );
}

function delete_msg() {
	// TODO 删除数据
	$(this).parents(".media").remove();
}