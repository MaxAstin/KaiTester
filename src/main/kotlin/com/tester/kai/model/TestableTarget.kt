package com.tester.kai.model

data class TestableTarget(
    val imports: List<String>,
    val isSuspend: Boolean,
    val functionCode: String,
    val classConstructor: String?,
    val className: String,
    val packageName: String,
)