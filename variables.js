var _ = Underscore.load();
scriptProperties = PropertiesService.getScriptProperties();
discordHeaders = {Authorization:'Bot '+scriptProperties.getProperty('discordBot'),'X-RateLimit-Precision':'millisecond'};
spreadsheet = SpreadsheetApp.getActive()
roster = spreadsheet.getSheetByName('Roster')
rosterData = roster.getDataRange()
columns = {
  name    : spreadsheet.getRangeByName('Name').getColumn(),
  active  : spreadsheet.getRangeByName('Active').getColumn(),
  guild   : spreadsheet.getRangeByName('Guild').getColumn(),
  level   : spreadsheet.getRangeByName('Ancestry').getColumn(),
  gear    : spreadsheet.getRangeByName('GearScore').getColumn(),
  class   : spreadsheet.getRangeByName('Occupation').getColumn(),
  updated : spreadsheet.getRangeByName('Updated').getColumn(),
  discord : spreadsheet.getRangeByName('Discord').getColumn(),
  image   : spreadsheet.getRangeByName('Image').getColumn(),
  notes   : spreadsheet.getRangeByName('Notes').getColumn(),
}
roles = {
  member : '620457035019976736',
  submit : '649369132361449492',
  friendnotfood : '620456021772599316',
  classes: {
    'Healer'    : '613140522680320056',
    'Mage'      : '613140575943655475',
    'Melee'     : '613140654981251092',
    'Frontline' : '616058792559378433',
    'Archer'    : '619216147912589313',
  },
  guilds: {
    'Impact'              : '647966393483919370',
    'Call Us Casual Kids' : '647966580394688522',
    'Moo'                 : '647966772409925638',
    'Beta Orbiters'       : '647966806346170370',
//  'Soup'                : '647967091235749890',
    'EGIRLS'              : '650847056851632157',
//  'Syndicate'           : '650701680953917468',
//  'Dragons Reign'       : '650950841787744272',
//  'RISE'                : '650950841787744272',
  }
}
guildNames = {
  'Impact'              : 'APES',
  'Call Us Casual Kids' : 'CUCK',
  'Moo'                 : 'Moo',
  'Beta Orbiters'       : 'Beta',
  'Soup'                : 'NEMO',
  'EGIRLS'              : 'EGIRLS',
  'Syndicate'           : 'Syndicate',
  'Dragons Reign'       : 'DR',
  'RISE'                : 'RISE',
}
permissions = {
  manageRoles: +268435456
}