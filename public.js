function sortRoster() {
  roster.sort(columns.name, true)
  roster.sort(columns.guild, true)
  roster.sort(columns.active, false)
}

function bulkUpdate() {
  var range = roster.getRange('A2:A')
  assignRoles({range: range});
}

function updateSelected() {
  var range = spreadsheet.getActiveRange()
  assignRoles({range: range});
}

function reminder() {
  bulkUpdate()
  discord("/channels/648377141234237463/messages",'post',{content: "<@&"+roles.submit+"> Daily reminder to submit a screenshot of your character info :)"})
}

function doGet(e) {
  var output = ContentService.createTextOutput().setMimeType(ContentService.MimeType.JSON)
  try {
    var user = findUser(e.parameter.search)
    if (!user) throw 'Could not find user matching "'+e.parameter.search+'"'
    console.info(user)
    for (i in user) if (user[i] === "") delete user[i]
    return outputUser(output,user)
  } catch (err) {
    return outputError(output,err)
  }
}

function doPost(e) {
  var output = ContentService.createTextOutput().setMimeType(ContentService.MimeType.JSON)
  try {
    if (e.parameter.token != scriptProperties.getProperty('apiToken')) return output
    
    var user = findUser(e.parameter.search)
    if (!user) throw 'Could not find user matching "'+e.parameter.search+'"'
    console.info(user)
    if (!user.discord) throw 'This character does not have a discord account assigned to it.\nPlease ask an administrator to create this'
    
    if (e.parameter.discord && e.parameter.discord != user.discord) {
      var hasPermission = false
      var discordUser = JSON.parse(discord('/guilds/613087331142074389/members/'+e.parameter.discord))
      var discordRoles = JSON.parse(discord('/guilds/613087331142074389/roles'))
      checkperms:
      for (i in discordRoles) {
        var discordRole = discordRoles[i]
        for (j in discordUser.roles) {
          var discordUserRole = discordUser.roles[j]
          if (discordUserRole == discordRole.id) {
            if (discordRole.permissions & permissions.manageRoles == permissions.manageRoles) {
              hasPermission = true
              break checkperms
        } } } }
      if (!hasPermission) throw "Sorry, you can only update your own character"
    }

    user = updateUser(user, e.parameter)

    for (i in user) if (user[i] === "") delete user[i]
    return outputUser(output,user)
  } catch (err) {
    return outputError(output,err)
  }
}