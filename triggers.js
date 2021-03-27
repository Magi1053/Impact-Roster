function assignRoles(e) {
  range = e.range
  sheetName = range.getSheet().getName()
  if (sheetName != roster.getName()) return

  row = range.getRow()
  lastRow = range.getLastRow()

  for ( row; row <= lastRow; row++) {
    if (row == 1) continue
    var user = getUser(row)
    if (!user.discord || isNaN(user.discord) || user.alt) continue
    console.log(user)
    //console.log('Row:'+(row), 'Name:'+user.name, 'Class:'+user.occupation, 'Guild:'+user.guild)

    var discordUser = JSON.parse(discord('/guilds/613087331142074389/members/'+user.discord))
    if (!discordUser.user) {
      //console.error("User does not exist in the discord server")
      continue
    }

    var classID = roles.classes[user.occupation]
    var guildID = roles.guilds[user.guild]
    var discordRoles = discordUser.roles.slice()

    if (!user.active) {
      for (i in discordRoles) {
        var role = discordRoles[i]
        if (role == roles.member) delete(discordRoles[i])
        if (role == roles.submit) delete(discordRoles[i])
        for (d in roles.guilds) if (role == roles.guilds[d]) {
          delete(discordRoles[i])
          continue
        }
      }
      discordRoles.push(roles.friendnotfood)
      discordRoles = _.unique(_.compact(discordRoles))
      if (!_.isEqual(discordRoles,discordUser.roles) || user.name != discordUser.nick) {
        console.warn("Removing user membership")
        discord('/guilds/613087331142074389/members/'+user.discord,'patch',{roles:discordRoles,nick:user.name,channel_id:null})
      }
      continue
    }

    for (i in discordRoles) {
      var role = discordRoles[i]
      for (d in roles.classes) {
        if (user.occupation == d) continue
        if (roles.classes[d] != role) continue
        delete(discordRoles[i])
      }
      for (d in roles.guilds) {
        if (user.guild == d) continue
        if (roles.guilds[d] != role) continue
        delete(discordRoles[i])
      }
      if (roles.friendnotfood == role) delete(discordRoles[i])
      if (roles.submit == role)
        if (user.submitted != false)
          delete(discordRoles[i])
    }

    discordRoles.push(roles.member)
    discordRoles.push(classID)
    discordRoles.push(guildID)
    if (user.submitted == false) discordRoles.push(roles.submit)
    discordRoles = _.unique(_.compact(discordRoles))
    if (!_.isEqual(discordRoles,discordUser.roles)) {
      console.warn("Modifying user roles")
      discord('/guilds/613087331142074389/members/'+user.discord,'patch',{roles:discordRoles})
    }
    if (user.nickname != discordUser.nick) {
      console.warn("Modifying user nickname")
      discord('/guilds/613087331142074389/members/'+user.discord,'patch',{nick:user.nickname})
    }

  }
}