package org.example.m42;

public class User {
	public static class UserInfo {
		private String handle;
		private int rating;
		private String rank;
		private String updatedAt;

		public String getHandle() {
			return handle;
		}
		public void setHandle(String handle) {this.handle = handle;}
		public int getRating() {
			return rating;
		}
		public void setRating(int rating) {
			this.rating = rating;
		}
		public String getRank() {
			return rank;
		}
		public void setRank(String rank) {
			this.rank = rank;
		}
		public String getUpdatedAt() {
			return updatedAt;
		}
		public void setUpdatedAt(String updatedAt) {
			this.updatedAt = updatedAt;
		}

	}

	public static class UserRating {
		private int contestId;
		private String contestName;
		private int rank;
		private int oldRating;
		private int newRating;
		private String ratingUpdatedAt;
		private String updatedAt;

		public int getContestId() { return contestId; }
		public void setContestId(int contestId) { this.contestId = contestId; }

		public String getContestName() { return contestName; }
		public void setContestName(String contestName) { this.contestName = contestName; }

		public int getRank() { return rank; }
		public void setRank(int rank) { this.rank = rank; }

		public int getOldRating() { return oldRating; }
		public void setOldRating(int oldRating) { this.oldRating = oldRating; }

		public int getNewRating() { return newRating; }
		public void setNewRating(int newRating) { this.newRating = newRating; }

		public String getRatingUpdatedAt() { return ratingUpdatedAt; }
		public void setRatingUpdatedAt(String ratingUpdatedAt) { this.ratingUpdatedAt = ratingUpdatedAt; }

		public String getUpdatedAt() { return updatedAt; }
		public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
	}
}

