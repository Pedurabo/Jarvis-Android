package com.example.gemmaruntimelab.ipc;

oneway interface IInferenceCallback {
    void onResult(String result);
}