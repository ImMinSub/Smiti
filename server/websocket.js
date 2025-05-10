const WebSocket = require('ws');

// 클라이언트로부터 메시지 수신 시 처리
wss.on('connection', (ws) => {
  ws.on('message', (message) => {
    try {
      // 원본 메시지 파싱
      const originalMessage = JSON.parse(message);

      // 서버 시간 추가 (UTC 밀리초 기준)
      originalMessage.timestamp = Date.now();

      const messageToSend = JSON.stringify(originalMessage);

      // 모든 클라이언트에게 브로드캐스트
      wss.clients.forEach((client) => {
        if (client.readyState === WebSocket.OPEN) {
          // 원본 메시지를 그대로 전달 (중요: sender_id, sender_name 등 모든 필드 유지)
          //client.send(message);
          client.send(messageToSend);
        }
      });

      // 데이터베이스에 메시지 저장
      saveMessage(originalMessage);

    } catch (error) {
      console.error('메시지 처리 중 오류:', error);
    }
  });
  ws.on('close', () => {
      console.log('클라이언트 연결 종료됨');
    });

    ws.on('error', (error) => {
      console.error('웹소켓 오류 발생:', error);
    });
});

console.log('WebSocket 서버 리스닝 중...');