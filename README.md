# ai-interview-platform

## KakaoPay Environment Variables

Backend reads KakaoPay settings only from environment variables.

- `KAKAO_PAY_ENABLED`
- `KAKAO_ADMIN_KEY`
- `KAKAO_PAY_CID`
- `KAKAO_PAY_BASE_URL`
- `KAKAO_PAY_CLIENT_BASE_URL`
- `KAKAO_PAY_CONNECT_TIMEOUT_MILLIS`
- `KAKAO_PAY_READ_TIMEOUT_MILLIS`

This project uses the Kakao Developers Admin Key flow.

- Authorization: `KakaoAK {KAKAO_ADMIN_KEY}`
- Base URL: `https://kapi.kakao.com`

It does not use the KakaoPay Open API Secret Key flow.

## Local Test Flow

1. Start backend and frontend with the KakaoPay environment variables set.
2. Add a book to the cart.
3. Go through `장바구니 -> 주문서 작성 -> 결제 진행`.
4. Choose `카카오페이` and start payment.
5. After KakaoPay redirects back to `/payment/result`, check:
   - order status
   - payment result message
   - payment history in `/orders`
6. For fallback testing, choose a mock payment method and verify success/fail buttons still work.
