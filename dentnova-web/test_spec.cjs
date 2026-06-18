fetch('https://kxuwskwwmrpoilrxngha.supabase.co/rest/v1/', {
  headers: {
    apikey: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imt4dXdza3d3bXJwb2lscnhuZ2hhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzk2NzU2NDIsImV4cCI6MjA5NTI1MTY0Mn0.KO749TxFU2o9oK2DH5OP40MIWClMKfXXr8OZlwNQhOE',
    Authorization: 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imt4dXdza3d3bXJwb2lscnhuZ2hhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzk2NzU2NDIsImV4cCI6MjA5NTI1MTY0Mn0.KO749TxFU2o9oK2DH5OP40MIWClMKfXXr8OZlwNQhOE'
  }
})
  .then(async r => {
    console.log('Status:', r.status);
    const text = await r.text();
    console.log('Body:', text.substring(0, 500));
  })
  .catch(console.error);
