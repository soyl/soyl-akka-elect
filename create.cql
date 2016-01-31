create table if not exists locks (
      name text,
      enabled boolean,
      lockttlsec int,
      checksec int,
      schedule text,
      last timestamp,
      lock uuid,
      state text,
      primary key (name)
);

