package com.netflix.contentservice.model;

/*
  It tracks the video processing life cycle
  PENDING -> UPLOADED -> ENCODING -> ENCODED -> READY
                                 -> FAILED
    DRAFT: Video is being created or updated, not yet ready for streaming
    PUBLISHED: Video is ready for streaming 
*/


public enum VideoStatus {
    PENDING,  // Initial state when video metadata is created but video file is not yet uploaded
    UPLOADED, // Video file has been uploaded, waiting for encoding to start
    ENCODING, // Video is currently being encoded
    ENCODED,  // Video has been successfully encoded
    READY,    // Video is ready for streaming
    FAILED    // Video processing has failed
}