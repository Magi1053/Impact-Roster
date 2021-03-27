function findUser(find) {
  if (!find || find == "") return
  find = find.toString().toLowerCase()
  if (!isNaN(find)) var data = roster.getRange(1,columns.discord,rosterData.getLastRow()).getValues()
  else              var data = roster.getRange(1,columns.name,rosterData.getLastRow()).getValues()
  for (var i in data) {
    var item = data[i][0].toLowerCase()
    if (item == find)  {
      var user = getUser(+i+1)
      if (user.alt == false) return user
    } }
  return null
}

function getUser(row) {
  var user = {
    row          : row,
    name         : rosterData.getCell(row,columns.name)   .getValue(),
    active       : rosterData.getCell(row,columns.active) .isChecked(),
    guild        : rosterData.getCell(row,columns.guild)  .getValue(),
    ancestry     : rosterData.getCell(row,columns.level)  .getValue(),
    equipment    : rosterData.getCell(row,columns.gear)   .getValue(),
    occupation   : rosterData.getCell(row,columns.class)  .getValue(),
    updated      : Date.parse(rosterData.getCell(row,columns.updated).getValue()),
    discord      : rosterData.getCell(row,columns.discord).getValue(),
    verification : rosterData.getCell(row,columns.image)  .getValue(),
    notes        : rosterData.getCell(row,columns.notes)  .getValue(),
    alt          : Boolean(rosterData.getCell(row,columns.discord).getFormula())
  }

  //if (!user.updated || user.updated > 2.16E7) { }

  var nameNote = rosterData.getCell(row,columns.name).getNote()
  if (nameNote != "") user.nickname = nameNote
  else user.nickname = user.name

  if (user.active && user.guild) {
    var deadline = new Date().setDate(new Date().getDate()-7)
    user.submitted = Boolean(user.equipment && user.updated && user.updated >= deadline)
    guildNote = rosterData.getCell(row,columns.guild).getNote()
    if (guildNote != "") user.nickname = "["+guildNote+"] "+user.nickname
    else user.nickname = "["+guildNames[user.guild]+"] "+user.nickname
  }

  return user
}

function updateUser(user,data) {
  console.warn("Updating user")
  var row = user.row
//if (data.name         != null)    rosterData.getCell(row,columns.name)   .setValue(data.name)
  if (data.active       == "false") rosterData.getCell(row,columns.active) .uncheck()
  else if (data.active  == "true")  rosterData.getCell(row,columns.active) .check()
  if (data.guild        != null)    rosterData.getCell(row,columns.guild)  .setValue(data.guild)
  if (data.ancestry     != null)    rosterData.getCell(row,columns.level)  .setValue(data.ancestry)
  if (data.equipment    != null)    rosterData.getCell(row,columns.gear)   .setValue(data.equipment)
  if (data.occupation   != null)    rosterData.getCell(row,columns.class)  .setValue(data.occupation)
//if (data.discord      != null)    rosterData.getCell(row,columns.discord).setValue(data.active)
  if (data.updated      != null)    rosterData.getCell(row,columns.updated).setValue(new Date(+data.updated))
//else                              rosterData.getCell(row,columns.updated).setValue(new Date())
  if (data.verification != null)    rosterData.getCell(row,columns.image)  .setValue(data.verification)
  if (data.notes        != null)    rosterData.getCell(row,columns.notes)  .setValue(data.notes)
  //assignRoles({range:rosterData.getCell(user.row,columns.name)})
  return getUser(user.row)
}

function outputError(output,message) {
  return output.setContent(JSON.stringify({error:message}))
}

function outputUser(output,data) {
  return output.setContent(JSON.stringify({user:data}))
}

function discord(route,method,options) {
  var requestUrl = "https://discordapp.com/api/"+route.replace(/^\/|\/$/g, '')
  var requestOpt = {
    method: method,
    payload: JSON.stringify(options),
    contentType: 'application/json',
    headers: discordHeaders,
    muteHttpExceptions: true
  }
  response = UrlFetchApp.fetch(requestUrl, requestOpt)
  var responseCode = response.getResponseCode()
  var responseText = response.getContentText()
  if (responseCode == 429) {
    var responseHeaders = response.getHeaders()
    var responseRateLimit = +responseHeaders['x-ratelimit-reset-after']
    console.error('Rate Limited for '+responseRateLimit+' seconds')
    Utilities.sleep(responseRateLimit*1000)
    discord(route,method,options)
  } else if (responseCode == 500) {
    console.error(responseText)
    Utilities.sleep(1000)
    discord(route,method,options)
  } else if (responseCode < 200 || responseCode >= 300) {
    console.error(responseText)
    try {
      var ui = spreadsheet.getUi();
      var uiResponse = ui.alert("Discord request failed with message:\n"+responseText+"\nWould you like to retry?", ui.ButtonSet.YES_NO);
    } catch (err) { }
    if (ui && uiResponse == ui.Button.YES) discord(route,method,options)
  }
  return response
}
