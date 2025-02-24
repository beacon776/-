package org.example.m42;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseService {
	static {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			e.getException();
		}
	}
	private static final String URL = "jdbc:mysql://localhost:3306/cf?useSSL=false&serverTimezone=UTC";
	private static final String USER = "root";
	private static final String PASSWORD = "123456";

	// 首先用数据库URL + name + password 建立与数据库的连接
	private static Connection getConnection() throws SQLException {
		return DriverManager.getConnection(URL, USER, PASSWORD);
	}

	// 插入用户信息，如果信息已经存在，就更新rating, rank, updated_at
	public static void insertUserInfo(String handle, int rating, String rank, String updateAt) throws SQLException {
		// SQL 查询：插入用户信息，如果 handle 相同则更新 rating、rank 和 updated_at 字段
		String sql = "INSERT INTO user_info (handle, rating, rank, updated_at) VALUES (?, ?, ?, ?) " +
				"ON DUPLICATE KEY UPDATE rating = VALUES(rating), rank = VALUES(rank), updated_at = VALUES(updated_at)";
		try (Connection connection = getConnection();
		     PreparedStatement statement = connection.prepareStatement(sql)) {
			// 设置 SQL 查询参数
			statement.setString(1, handle);
			statement.setInt(2, rating);
			statement.setString(3, rank);
			statement.setString(4, updateAt);
			// 执行更新或插入操作
			statement.executeUpdate();
		} catch (SQLException e) {
			System.err.println("Database error: " + e.getMessage());
			e.printStackTrace();
		}
	}

	// 插入用户比赛成绩数据，若该记录已存在，则更新相关字段
	public static void insertUserRating(String handle, int contestId, String contestName, int rank, int oldRating, int newRating, String ratingUpdateAt, String updateAt) {
		// SQL 查询：插入用户比赛成绩数据，如果 handle 和 contest_id 相同则更新相关字段
		String sql = "INSERT INTO user_rating (handle, contest_id, contest_name, rank, old_rating, new_rating, rating_updated_at, updated_at) " +
				"VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
				"ON DUPLICATE KEY UPDATE rank = VALUES(rank), old_rating = VALUES(old_rating)," +
				"new_rating = VALUES(new_rating), rating_updated_at = VALUES(rating_updated_at), updated_at = VALUES(updated_at)";
		try (Connection connection = getConnection();
		     PreparedStatement statement = connection.prepareStatement(sql)) {
			// 设置 SQL 查询参数
			statement.setString(1, handle);
			statement.setInt(2, contestId);
			statement.setString(3, contestName);
			statement.setInt(4, rank);
			statement.setInt(5, oldRating);
			statement.setInt(6, newRating);
			statement.setString(7, ratingUpdateAt);
			statement.setString(8, updateAt);
			// 执行更新或插入操作
			statement.executeUpdate();
		} catch (SQLException e) {
			System.err.println("Database error: " + e.getMessage());
			e.printStackTrace();
		}
	}

	// 根据 handle 获取用户信息
	public static User.UserInfo getUserInfo(String handle) {
		// SQL 查询：根据 handle 获取用户信息
		String sql = "SELECT * FROM user_info WHERE handle = ?";
		try (Connection connection = getConnection();
		     PreparedStatement statement = connection.prepareStatement(sql)) {
			// 设置 SQL 查询参数
			statement.setString(1, handle);
			try (ResultSet resultSet = statement.executeQuery()) {
				// 如果查询结果存在，返回用户信息 这里只用面对单个对象，并且单个handle的信息是唯一的，所以只需要设置成if
				if (resultSet.next()) {
					User.UserInfo userInfo = new User.UserInfo();
					userInfo.setHandle(resultSet.getString("handle"));
					userInfo.setRating(resultSet.getInt("rating"));
					userInfo.setRank(resultSet.getString("rank"));
					userInfo.setUpdatedAt(resultSet.getString("updated_at"));
					return userInfo;
				}
			}
		} catch (SQLException e) {
			System.err.println("Database error: " + e.getMessage());
			e.printStackTrace();
		}
		// 如果没有查询到用户信息，返回 null
		return null;
	}

	// 根据 handle 获取用户的比赛成绩数据，返回一个用户的所有比赛成绩
	public static List<User.UserRating> getUserRating(String handle) {
		// SQL 查询：根据 handle 获取该用户的所有比赛成绩
		String sql = "SELECT * FROM user_rating WHERE handle = ?";
		List<User.UserRating> ratings = new ArrayList<>();
		try (Connection connection = getConnection();
		     PreparedStatement statement = connection.prepareStatement(sql)) {
			// 设置 SQL 查询参数
			statement.setString(1, handle);
			// 执行查询操作
			try (ResultSet resultSet = statement.executeQuery()) {
				// 遍历查询结果并将每条记录转化为 UserRating 对象
				while (resultSet.next()) {
					User.UserRating rating = new User.UserRating();
					rating.setContestId(resultSet.getInt("contest_id"));
					rating.setContestName(resultSet.getString("contest_name"));
					rating.setRank(resultSet.getInt("rank"));
					rating.setOldRating(resultSet.getInt("old_rating"));
					rating.setNewRating(resultSet.getInt("new_rating"));
					rating.setRatingUpdatedAt(resultSet.getString("rating_updated_at"));
					rating.setUpdatedAt(resultSet.getString("updated_at"));
					// 将结果添加到 ratings 列表
					ratings.add(rating);
				}
			}
		} catch (SQLException e) {
			System.err.println("Database error: " + e.getMessage());
			e.printStackTrace();
		}
		// 返回用户的所有比赛成绩
		return ratings;
	}
}

