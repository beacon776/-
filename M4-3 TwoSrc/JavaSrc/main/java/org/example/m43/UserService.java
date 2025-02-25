package org.example.m43;

import org.json.JSONArray;
import org.json.JSONObject;
// 注意，import json的同时，也需要在maven中加入json依赖哦
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class UserService {

	public static JSONObject getUserInformation(String handle) throws IOException {
		// 查询数据库
		try {
			List<User.UserInfo> ratings = DatabaseService.getUserInfo(handle);
			// 数据库里有信息
			if (ratings != null && !ratings.isEmpty()) {
				long currentTime = System.currentTimeMillis();
				long lastUpdatedTime = Timestamp.valueOf(ratings.get(0).getUpdatedAt()).getTime();
				// 30秒内数据有效
				if ((currentTime - lastUpdatedTime) <= 30 * 1000) {
					JSONObject userInfo = new JSONObject();
					for (User.UserInfo rating : ratings) {
						userInfo.put("handle", rating.getHandle());
						userInfo.put("rating", rating.getRating());
						userInfo.put("rank", rating.getRank());
						userInfo.put("updatedAt", rating.getUpdatedAt());
					}
					return userInfo;
				}
			}
		} catch (Exception e) {
			System.err.println("Error fetching user info from database: " + e.getMessage());
			e.printStackTrace();
		}

		// 数据过期或者不存在，调用API
		JSONObject apiData = getUserInformationFromAPI(handle);
		if (apiData.getBoolean("success")) {
			try {
				String updatedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
				// 更新数据库
				DatabaseService.insertUserInfo(handle, apiData.getInt("rating"), apiData.getString("rank"), updatedAt);

				apiData.put("updatedAt", updatedAt);
			} catch (SQLException e) {
				System.err.println("Error inserting user info into database: " + e.getMessage());
				e.printStackTrace();
			}
		}
		return apiData;
	}

	public static JSONArray getUserRating(String handle) throws IOException {
		// 检查数据库
		try {
			List<User.UserRating> ratings = DatabaseService.getUserRating(handle);
			if (ratings != null && !ratings.isEmpty()) {
				long currentTime = System.currentTimeMillis();
				long lastUpdatedTime = Timestamp.valueOf(ratings.get(0).getUpdatedAt()).getTime();
				// 30秒内数据有效
				if ((currentTime - lastUpdatedTime) <= 30 * 1000) {
					JSONArray resultArray = new JSONArray();
					for (User.UserRating rating : ratings) {
						JSONObject ratingData = new JSONObject();
						ratingData.put("contestId", rating.getContestId());
						ratingData.put("contestName", rating.getContestName());
						ratingData.put("rank", rating.getRank());
						ratingData.put("oldRating", rating.getOldRating());
						ratingData.put("newRating", rating.getNewRating());
						ratingData.put("ratingUpdatedAt", rating.getRatingUpdatedAt()); // String
						ratingData.put("updatedAt", rating.getUpdatedAt()); // String
						resultArray.put(ratingData);
					}
					return resultArray;
				}
			}
		} catch (Exception e) {
			System.err.println("Error fetching user rating from database: " + e.getMessage());
			e.printStackTrace();
		}

		// 数据过期或者不存在，调用API
		JSONArray apiData = getUserRatingFromAPI(handle);
		if (apiData != null) {
			try {
				for (int i = 0; i < apiData.length(); i++) {
					JSONObject rating = apiData.getJSONObject(i);
					System.out.println("rating[" + i + "] = " + rating);

					// 确保字段存在
					if (!rating.has("ratingUpdatedAt")) {
						System.err.println("Error: ratingUpdateTimeSeconds field not found in API response.");
						continue; // 跳过当前记录
					}

					// 注意，我这里在api调用时已经提前封装好这是个yyy-MM-dd HH:mm:ss格式的String了，这里不用再手动转换了
					String ratingUpdatedAt = rating.getString("ratingUpdatedAt");

					// 统一时间格式
					String updatedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

					// 插入数据库
					DatabaseService.insertUserRating(
							handle, rating.getInt("contestId"), rating.getString("contestName"),
							rating.getInt("rank"), rating.getInt("oldRating"),
							rating.getInt("newRating"),
							ratingUpdatedAt, updatedAt
					);
					rating.put("ratingUpdatedAt", ratingUpdatedAt);
					rating.put("updatedAt", updatedAt);
				}
				return apiData;
			} catch (Exception e) {
				System.err.println("Error inserting user rating into database: " + e.getMessage());
				e.printStackTrace();
			}
			return apiData;
		}
		return null;
	}


	// 查询单个handle的信息的方法和它对应的报错。
	public static JSONObject getUserInformationFromAPI(String handle) throws IOException {
		// 用UTF-8维护 中文 or 空格的查询(虽然cf的handle应该没有这种东西，但是如果真的有人这么输入的话还是得处理一下)
		String encodedHandle = URLEncoder.encode(handle, "UTF-8");
		String apiUrl = "https://codeforces.com/api/user.info?handles=" + encodedHandle;// 拼接API请求URL
		try {
			// 重头戏:解析 JSON 数据
			// 注意，虽然StringBuilder类看起来是没有改变字符串本身的，但是它返回的是String形式，我们需要String类型的参数才能转到JSONObject类型。
			JSONObject jsonResponse = makeApiRequest(apiUrl);
			JSONObject data = new JSONObject();
			System.out.println("API Response: " + jsonResponse);  // 调试输出，方便我们自己检查

			// JSONObject的底层用HashMap维护的，键值是对应的，可以用get去查询key对应的value。
			if("OK".equals(jsonResponse.getString("status"))) {
				JSONArray jsonArray = jsonResponse.getJSONArray("result");
				if(!jsonArray.isEmpty()) { // 情况1:handle可以正常找到
					JSONObject temp = jsonArray.getJSONObject(0);
					data.put("success", true)
							.put("handle", handle);

					if(temp.has("rating") && !temp.isNull("rating")) { // 情况1.2:存在rating和rank
						data.put("rating", temp.getInt("rating"));
					}
					if(temp.has("rank") && !temp.isNull("rank")) {
						data.put("rank", temp.getString("rank"));
					}
					return data;
				} else { // 情况2:handle无法找到
					return new JSONObject()
							.put("success", false)
							.put("type", 1)
							.put("message", "no such handle");
				}
			} else { //情况4:在查询此项时未收到有效 HTTP 响应
				return new JSONObject()
						.put("success", false)
						.put("type", 3)
						.put("message", jsonResponse.toString());
			}
		} catch(IOException e) {
			// 情况3:在查询此项时遭遇异常 HTTP 响应
			JSONObject errorData = new JSONObject();
			errorData.put("success", false)
					.put("type", 2)
					.put("message", e.getMessage())
					.put("details", new JSONObject().put("status", 503));
			// HTTP状态码 503是服务器无法处理请求时返回的一般错误响应
			return errorData;
		}
	}
	// 调用上面的那个处理单个handle的方法，我们可以返回多个handle对应的json数据
	public static JSONArray getMultipleUserInformation(String handles) {
		JSONArray resultArray = new JSONArray();
		String[] handlesArray = handles.trim().split(",");

		for(String handle : handlesArray) {
			if(handle.matches("[a-zA-Z0-9_-]+")) {
				try {
					JSONObject result = getUserInformation(handle);
					resultArray.put(result);
				} catch (IOException e) {
					// 情况 3：在查询此项时遭遇异常 HTTP 响应
					JSONObject errorData = new JSONObject()
							.put("success", false)
							.put("type", 2)
							.put("message", "HTTP response with code 503")
							.put("details", new JSONObject().put("status", 503));
					resultArray.put(errorData);
				} catch (Exception e) { // 情况 5：在查询此项时程序发生运行时异常
					JSONObject errorData = new JSONObject()
							.put("success", false)
							.put("type", 4)
							.put("message", "Internal Server Error");
					resultArray.put(errorData);
				}
			} else {
				JSONObject errorData = new JSONObject()
						.put("success", false)
						.put("type", 5)
						.put("message", "Invalid handle format: " + handle);
				resultArray.put(errorData);
			}
		}
		return resultArray;
	}

	// 调用API查rating
	public static JSONArray getUserRatingFromAPI(String handle) throws IOException {
		String encodedHandle = URLEncoder.encode(handle, "UTF-8");
		String apiUrl = "https://codeforces.com/api/user.rating?handle=" + encodedHandle;
		try {
			JSONArray resultArray = new JSONArray(); // 真正储存结果
			JSONObject ratingData = makeApiRequest(apiUrl); // 获取比赛数据

			System.out.println("API Response: " + ratingData);  // 打印输出，方便我们自己检查

			if ("OK".equals(ratingData.getString("status"))) { // status为OK的话，请求成功，说明这是有handle的情况
				JSONArray jsonArray = ratingData.getJSONArray("result");
				if (!jsonArray.isEmpty()) {  // 如果有比赛记录
					for (int i = 0; i < jsonArray.length(); i++) {
						JSONObject record = jsonArray.getJSONObject(i); // 比赛记录的原始json数据
						JSONObject data = new JSONObject(); // 对原始数据修改一下再重新存

						// 确保字段存在
						if (!record.has("ratingUpdateTimeSeconds")) {
							System.err.println("Error: ratingUpdateTimeSeconds field not found in API response.");
							continue; // 跳过当前记录
						}

						// 以下五行是把long 类型的时间戳转化为标准时间的步骤
						long time = record.getLong("ratingUpdateTimeSeconds");
						Instant instant = Instant.ofEpochSecond(time);
						LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
						DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
						String formattedDate = localDateTime.format(formatter);

						// 查询record中的数据并给data增加对应的数据
						data.put("handle", handle);
						data.put("contestId", record.getInt("contestId"))
								.put("contestName", record.getString("contestName"))
								.put("rank", record.getInt("rank"))
								.put("ratingUpdatedAt", formattedDate)  // 这里注意原来是long类型的时间戳格式的，我需要把它转化成标准时间
								.put("oldRating", record.getInt("oldRating"))
								.put("newRating", record.getInt("newRating"));
						resultArray.put(data);
					}
				} else {
					// 无handle + 该handle无比赛记录的情况，给出报错
					JSONObject errorData = new JSONObject().put("message", "'no such handle'");
					resultArray.put(errorData);
				}
			} else {
				// API响应状态不是OK，给出报错
				JSONObject errorData = new JSONObject().put("message", "Error:" + ratingData.getString("status")); //API 响应的 status 不是OK时，给它明确的错误信息
				resultArray.put(errorData);
			}
			return resultArray;
		} catch (IOException e) {
			// 如果再调用API或者处理过程中发生IO异常，给出报错
			JSONArray errorArray = new JSONArray();
			JSONObject errorData = new JSONObject().put("message", e.getMessage());
			errorArray.put(errorData);
			return errorArray;
		}
	}

	// 两个查询的前半段重复了好多，我把他们提取出来了，弄成了以下这个url转JSONObject的方法。
	private static JSONObject makeApiRequest(String apiUrl) throws IOException {
		HttpURLConnection connection = null;
		try {
			// 注意connection那里不能写注释，要不会connection会读取注释的信息......
			URL url = new URL(apiUrl);
			connection = (HttpURLConnection) url.openConnection(); // url 转 HttpURLConnection
			connection.setRequestMethod("GET");
			connection.setConnectTimeout(5000);
			connection.setReadTimeout(5000);

			// 处理响应的状态码
			int status = connection.getResponseCode();
			System.out.println("HTTP Status Code: " + status);  // 在终端这里输出HTTP状态码，方便我们自己查看结果啦。这样不仅能在浏览器上看，也能在程序这里看到结果了。
			if (status != HttpURLConnection.HTTP_OK) {
				throw new IOException("HTTP response with code " + status);
			}

			// 读取响应内容，用缓冲流 + 字节流处理读入的数据
			/* 我们已知信息就是纯文本的形式，就直接用字节流没有问题的啦*/
			StringBuilder result = new StringBuilder();
			// 新添加了 try-with-resources 形式的写法
			try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
				String line;
				while ((line = in.readLine()) != null) {
					result.append(line);// append支持append一个String，StringBuilder不能直接与String相加
				}
			}
			System.out.println("Response Body: " + result);  // 打印响应内容，方便我们自己检查。
			return new JSONObject(result.toString());
		} finally {
			if (connection != null && connection.getInputStream() != null) {
				connection.disconnect();// 去读了一下源码，disconnect()方法的作用是让这个HttpURLConnection实例不再被复用
			}
		}
	}
}

