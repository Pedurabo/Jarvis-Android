package com.example.gemmaruntimelab.ipc;

import com.example.gemmaruntimelab.ipc.IInferenceCallback;

interface ICompanionProbe {
    String ping(String message);
    String getModelStatus();
    void initializeModel(IInferenceCallback callback);
    void generateResponse(String prompt, IInferenceCallback callback);
}