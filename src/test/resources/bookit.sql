select firstname,lastname,role from users
where role='student-team-leader';

select name, batch_number, c.location from team t left join batch b
    on t.batch_number = b.number
left join campus c
    on t.campus_id = c.id
where t.id = 17166;
