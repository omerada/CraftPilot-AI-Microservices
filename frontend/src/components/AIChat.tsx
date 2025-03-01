import React, { useState } from "react";
import axios from "axios";

interface ChatProps {
  userId: string;
  userRole: string;
  userEmail: string;
  token: string;
}

const AIChat: React.FC<ChatProps> = ({
  userId,
  userRole,
  userEmail,
  token,
}) => {
  const [prompt, setPrompt] = useState("");
  const [response, setResponse] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  const headers = {
    "Content-Type": "application/json",
    Authorization: `Bearer ${token}`,
    "X-User-Id": userId,
    "X-User-Role": userRole,
    "X-User-Email": userEmail,
  };

  // Normal endpoint kullanımı
  const sendNormalRequest = async () => {
    setIsLoading(true);
    try {
      const { data } = await axios.post(
        "https://api.craftpilot.io/ai/chat/completions",
        {
          prompt,
          model: "google/gemini-pro",
          temperature: 0.7,
          maxTokens: 2000,
          requestType: "CHAT",
        },
        { headers }
      );

      setResponse(data.response);
    } catch (error) {
      console.error("Error:", error);
      setResponse(
        axios.isAxiosError(error)
          ? `Hata: ${error.response?.data?.error || error.message}`
          : "Bir hata oluştu."
      );
    } finally {
      setIsLoading(false);
    }
  };

  // Streaming endpoint kullanımı
  const sendStreamRequest = async () => {
    setResponse("");
    try {
      const { data } = await axios.post(
        "https://api.craftpilot.io/ai/chat/completions/stream",
        {
          prompt,
          model: "google/gemini-pro",
          temperature: 0.7,
          maxTokens: 2000,
          requestType: "CHAT",
        },
        {
          headers: {
            ...headers,
            Accept: "text/event-stream",
          },
          responseType: "stream",
          onDownloadProgress: (progressEvent) => {
            const chunk = progressEvent.event.target.response;
            if (chunk) {
              try {
                const data = JSON.parse(chunk);
                if (data.response) {
                  setResponse((prev) => prev + data.response);
                }
              } catch (e) {
                // SSE chunk parsing hatası
                console.debug("Invalid chunk:", chunk);
              }
            }
          },
        }
      );
    } catch (error) {
      console.error("Stream error:", error);
      setResponse(
        axios.isAxiosError(error)
          ? `Stream Hatası: ${error.response?.data?.error || error.message}`
          : "Stream bağlantısında hata oluştu."
      );
    }
  };

  return (
    <div>
      <textarea
        value={prompt}
        onChange={(e) => setPrompt(e.target.value)}
        placeholder="Sorunuzu yazın..."
      />
      <div>
        <button onClick={sendNormalRequest} disabled={isLoading}>
          Normal Gönder
        </button>
        <button onClick={sendStreamRequest} disabled={isLoading}>
          Stream İle Gönder
        </button>
      </div>
      <div>
        {isLoading && <div>Yanıt bekleniyor...</div>}
        <pre>{response}</pre>
      </div>
    </div>
  );
};

export default AIChat;
