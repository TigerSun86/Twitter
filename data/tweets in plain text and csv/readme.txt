Author screen name to its user id:

CLIMATE_DESK = 120181944
CLIMATE_PROGRESS = 28657802
CLIMATE_REALITY = 16958346
EARTH_VITAL_SIGNS = 15461733
GREEN_PEACE = 3459051
UNEP = 38146999
UNFCCC = 17463923

The tweets are stored in the format of json and seperated by newline (\r\n). Newline is the character never appearing in the twwet content, as in:

https://dev.twitter.com/streaming/overview/processing

The body of a streaming API response consists of a series of newline-delimited messages, where ¡°newline¡± is considered to be \r\n (in hex, 0x0D 0x0A) and ¡°message¡± is a JSON encoded data structure or a blank line.

Note that Tweet content may sometimes contain linefeed \n characters, but will not contain carriage returns \r. Therefore, to make sure you get whole message payloads, break out each message on \r\n boundaries, as \n may occur in the middle of a message. 