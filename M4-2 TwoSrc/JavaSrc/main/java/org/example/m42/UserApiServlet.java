package org.example.m42;

import jakarta.servlet.http.HttpServlet;
import org.json.JSONArray;
import org.json.JSONObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// 用注解映射到 /batchGetUserInfo路径
@WebServlet("/batchGetUserInfo")
public class UserApiServlet extends HttpServlet {
	@Override
	// 处理get请求
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		// 设置允许跨域访问
		response.setHeader("Access-Control-Allow-Origin", "*");
		// 返回内容格式为JSON格式
		response.setHeader("Content-Type", "application/json");
		// 获取handles参数
		String handles = request.getParameter("handles");
		// 异常判断
		if (handles == null) {
			sendUserErrorResponse(response, 400, "Missing handles query parameter");
			return;
		}
		try {
			// 直接调用之前写好的方法获取数据
			JSONArray responseArray = UserService.getMultipleUserInformation(handles);
			// 设置响应体
			response.getWriter().write(responseArray.toString());
		} catch (Exception e) {
			sendUserErrorResponse(response, 500, "Internal Server Error");
		}
	}


	private void sendUserErrorResponse(HttpServletResponse response, int code, String msg) throws IOException {
		response.setStatus(code);
		response.getWriter().write(new JSONObject()
				.put("success", false)
				.put("type", 4)
				.put("message", msg)
				.toString());
	}
}
