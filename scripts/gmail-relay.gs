/**
 * Rahul Library: Gmail relay for password reset codes.
 *
 * Runs in the Google account rahul.vachanalaya@gmail.com as a Google Apps Script web app. The server
 * (EmailSender.java) posts {secret, to, toName, subject, text} here over HTTPS, and this sends it with
 * that account's Gmail, so the email really comes from Gmail and lands in the inbox, not in spam.
 *
 * Setup (once):
 *   1. script.google.com, signed in as rahul.vachanalaya@gmail.com → New project → paste this file.
 *   2. Project Settings (gear) → Script properties → Add: RELAY_SECRET = a long random phrase.
 *   3. Deploy → New deployment → type "Web app"; Execute as: Me; Who has access: Anyone → Deploy,
 *      allow the Gmail permission, and copy the web app URL (ends in /exec).
 *   4. On Render (ngo-library-api → Environment): GMAIL_RELAY_URL = that URL,
 *      GMAIL_RELAY_SECRET = the same phrase as RELAY_SECRET.
 * After editing this file: Deploy → Manage deployments → edit → Version: New version, so the URL stays the same.
 *
 * Free Gmail accounts can send about 100 emails a day this way.
 */
function doPost(e) {
  var reply = function (body) {
    return ContentService.createTextOutput(JSON.stringify(body)).setMimeType(ContentService.MimeType.JSON);
  };
  var expected = PropertiesService.getScriptProperties().getProperty('RELAY_SECRET');
  var msg;
  try {
    msg = JSON.parse(e.postData.contents);
  } catch (err) {
    return reply({ ok: false, error: 'bad request' });
  }
  // Only the library's server knows the secret; anyone else gets nothing sent.
  if (!expected || msg.secret !== expected) {
    return reply({ ok: false, error: 'forbidden' });
  }
  if (!msg.to || !msg.subject || !msg.text) {
    return reply({ ok: false, error: 'missing to, subject or text' });
  }
  try {
    MailApp.sendEmail({ to: msg.to, subject: msg.subject, body: msg.text, name: 'Rahul Library' });
    return reply({ ok: true, left: MailApp.getRemainingDailyQuota() });
  } catch (err) {
    return reply({ ok: false, error: String(err) });
  }
}
