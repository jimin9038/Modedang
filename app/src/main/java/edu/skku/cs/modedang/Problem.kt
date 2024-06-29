package edu.skku.cs.modedang

data class Problem(
    val id: Int,
    val title: String,
    val engTitle: String?,
    val difficulty: String,
    val submissionCount: Int,
    val acceptedRate: Double,
    val tags: List<Tag>
)

data class Tag(
    val id: Int,
    val name: String
)
